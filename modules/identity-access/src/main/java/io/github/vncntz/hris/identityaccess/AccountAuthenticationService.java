package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** A short row-locked transaction serializes password attempts for one account. */
@Service
public class AccountAuthenticationService {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final SecurityPolicy policy;
    private final String dummyHash;

    AccountAuthenticationService(AccountRepository accounts, PasswordEncoder encoder,
                                 Clock clock, SecurityPolicy policy) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.clock = clock;
        this.policy = policy;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<AuthenticatedAccount> authenticate(String suppliedLogin, String rawPassword) {
        String canonical;
        try {
            canonical = LoginNames.canonicalize(suppliedLogin);
        } catch (IllegalArgumentException invalid) {
            encoder.matches(rawPassword, dummyHash);
            return Optional.empty();
        }

        Optional<AccountEntity> found = accounts.findByCanonicalLogin(canonical);
        if (found.isEmpty()) {
            encoder.matches(rawPassword, dummyHash);
            return Optional.empty();
        }

        AccountEntity account = found.orElseThrow();
        Instant now = clock.instant();
        boolean matches = encoder.matches(rawPassword, account.passwordHash());
        if (!account.enabled() || account.isLockedAt(now)) {
            return Optional.empty();
        }
        account.clearExpiredLock(now);
        if (!matches) {
            account.recordFailure(now, policy.maxFailedAttempts(), policy.lockDuration());
            return Optional.empty();
        }
        account.recordSuccess(now);
        List<String> authorityKeys = accounts.findAssignedRoles(account.id()).stream()
                .filter(RoleEntity::enabled)
                .flatMap(role -> role.authorityKeys().stream())
                .distinct().sorted().toList();
        return Optional.of(new AuthenticatedAccount(
                new AccountPrincipal(account.publicId(), account.canonicalLogin()), authorityKeys, account.authenticationGeneration()));
    }
}

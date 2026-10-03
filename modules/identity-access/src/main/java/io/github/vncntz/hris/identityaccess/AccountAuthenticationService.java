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
    private final MfaVerifier mfa;
    private final jakarta.persistence.EntityManager entities;

    AccountAuthenticationService(AccountRepository accounts, PasswordEncoder encoder,
                                 Clock clock, SecurityPolicy policy, MfaVerifier mfa,
                                 jakarta.persistence.EntityManager entities) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.clock = clock;
        this.policy = policy;
        this.mfa = mfa;
        this.entities = entities;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<AuthenticatedAccount> authenticate(String suppliedLogin, String rawPassword) {
        return authenticate(suppliedLogin, rawPassword, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<AuthenticatedAccount> authenticate(String suppliedLogin, String rawPassword, char[] factor) {
        try {
            return verify(suppliedLogin, rawPassword, factor);
        } finally { InitialCredentials.clear(factor); }
    }

    private Optional<AuthenticatedAccount> verify(String suppliedLogin, String rawPassword, char[] factor) {
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
        entities.refresh(account);
        Instant now = clock.instant();
        boolean matches = encoder.matches(rawPassword, account.passwordHash());
        if (!account.enabled() || account.isLockedAt(now)) {
            return Optional.empty();
        }
        account.clearExpiredLock(now);
        boolean factorMatches = false;
        if (matches) {
            try { factorMatches = mfa.verify(account, factor, now); }
            catch (MfaException unavailable) { factorMatches = false; }
        }
        if (!matches || !factorMatches) {
            account.recordFailure(now, policy.maxFailedAttempts(), policy.lockDuration());
            return Optional.empty();
        }
        account.recordSuccess(now);
        List<RoleEntity> assigned = accounts.findAssignedRoles(account.id());
        RoleGenerations generations = new RoleGenerations(assigned.stream().collect(
                java.util.stream.Collectors.toMap(RoleEntity::publicId, RoleEntity::authorizationGeneration)));
        List<String> authorityKeys = assigned.stream()
                .filter(RoleEntity::enabled)
                .flatMap(role -> role.authorityKeys().stream())
                .distinct().sorted().toList();
        return Optional.of(new AuthenticatedAccount(
                new AccountPrincipal(account.publicId(), account.canonicalLogin()), authorityKeys,
                account.authenticationGeneration(), generations));
    }
}

package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static io.github.vncntz.hris.identityaccess.RecentAuthenticationException.Reason.*;

/** CurrentActor-only credential proof using the shared Account authentication policy. */
@Service
public class CredentialReauthenticationService {
    private final CurrentActor actor;
    private final RecentAuthenticationSession session;
    private final AccountRepository accounts;
    private final EntityManager entities;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final SecurityPolicy policy;
    private final TransactionTemplate verification;

    CredentialReauthenticationService(CurrentActor actor, RecentAuthenticationSession session,
            AccountRepository accounts, EntityManager entities, PasswordEncoder encoder,
            Clock clock, SecurityPolicy policy, PlatformTransactionManager transactions) {
        this.actor = actor;
        this.session = session;
        this.accounts = accounts;
        this.entities = entities;
        this.encoder = encoder;
        this.clock = clock;
        this.policy = policy;
        verification = new TransactionTemplate(transactions);
        verification.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verification.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        verification.setTimeout(15);
    }

    /** Takes ownership, clearing the supplied buffer even on session/authentication denial. */
    public void reauthenticate(char[] password) {
        try {
            // The session boundary clears old proof before any validation and serializes attempts.
            session.attempt(() -> verify(password));
        } catch (RecentAuthenticationException failure) {
            throw failure;
        } catch (org.springframework.security.core.AuthenticationException failure) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Authentication required");
        } catch (RuntimeException failure) {
            throw new RecentAuthenticationException(PERSISTENCE_FAILED);
        } finally {
            InitialCredentials.clear(password);
        }
    }

    private RecentAuthenticationSession.Proof verify(char[] password) {
        UUID id = actor.requireUserId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RecentAuthenticationException(PERSISTENCE_FAILED);
        }
        if (password == null) { throw new RecentAuthenticationException(CREDENTIAL_REJECTED); }
        var snapshot = accounts.findEnabledAuthenticationState(id).orElseThrow(() ->
                new RecentAuthenticationException(ACCOUNT_UNAVAILABLE));
        if (snapshot.getLockedUntilUtc() != null
                && clock.instant().isBefore(snapshot.getLockedUntilUtc().toInstant(ZoneOffset.UTC))) {
            throw new RecentAuthenticationException(CREDENTIAL_REJECTED);
        }
        String verified = snapshot.getPasswordHash();
        boolean matches = encoder.matches(CharBuffer.wrap(password), verified);
        // Execute returns only after commit; a commit failure cannot publish proof.
        Optional<RecentAuthenticationSession.Proof> result = verification.execute(status -> {
            AccountEntity account = accounts.findByPublicId(id).orElseThrow(() ->
                    new RecentAuthenticationException(ACCOUNT_UNAVAILABLE));
            entities.refresh(account);
            if (!account.enabled()) { throw new RecentAuthenticationException(ACCOUNT_UNAVAILABLE); }
            if (!verified.equals(account.passwordHash())) {
                throw new RecentAuthenticationException(STALE_CREDENTIAL);
            }
            Instant now = clock.instant();
            if (account.isLockedAt(now)) { throw new RecentAuthenticationException(CREDENTIAL_REJECTED); }
            account.clearExpiredLock(now);
            if (!matches) {
                account.recordFailure(now, policy.maxFailedAttempts(), policy.lockDuration());
                // Return normally so the failure bookkeeping commits before rejection.
                return Optional.empty();
            }
            account.recordSuccess(now);
            return Optional.of(new RecentAuthenticationSession.Proof(id, now));
        });
        return result.orElseThrow(() -> new RecentAuthenticationException(CREDENTIAL_REJECTED));
    }
}

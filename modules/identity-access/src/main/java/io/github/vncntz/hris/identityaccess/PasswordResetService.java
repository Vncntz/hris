package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Authenticated administrative reset of another Account, including disabled Accounts. */
@Service
public class PasswordResetService {
    private final CurrentActor actor;
    private final RecentAuthenticationGuard recent;
    private final AccountRepository accounts;
    private final EntityManager entities;
    private final PasswordEncoder encoder;
    private final AuditRecorder audit;
    private final AuthenticatedSessionRevoker sessions;
    private final Clock clock;
    private final TransactionTemplate mutation;

    PasswordResetService(CurrentActor actor, RecentAuthenticationGuard recent, AccountRepository accounts,
            EntityManager entities, PasswordEncoder encoder, AuditRecorder audit,
            AuthenticatedSessionRevoker sessions, Clock clock, PlatformTransactionManager transactions) {
        this.actor = actor;
        this.recent = recent;
        this.accounts = accounts;
        this.entities = entities;
        this.encoder = encoder;
        this.audit = audit;
        this.sessions = sessions;
        this.clock = clock;
        mutation = new TransactionTemplate(transactions);
        mutation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        mutation.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        mutation.setTimeout(15);
    }

    /** Takes ownership of both secret buffers on every success or failure path. */
    public void reset(UUID target, char[] newPassword, char[] confirmation) {
        try {
            actor.requireAuthority("identity:admin");
            recent.requireRecentAuthentication();
            UUID administrator = actor.requireUserId();
            if (target == null) {
                throw new PasswordResetException(PasswordResetException.Reason.INVALID_TARGET);
            }
            if (administrator.equals(target)) {
                throw new PasswordResetException(PasswordResetException.Reason.SELF_RESET_REJECTED);
            }
            try {
                InitialCredentials.validate(newPassword, confirmation);
            } catch (IllegalArgumentException failure) {
                throw new PasswordResetException(PasswordResetException.Reason.INVALID_CREDENTIAL);
            }
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new PasswordResetException(PasswordResetException.Reason.PERSISTENCE_FAILED);
            }
            long observed;
            try {
                observed = accounts.findAuthenticationGeneration(target).orElseThrow(() ->
                        new PasswordResetException(PasswordResetException.Reason.ACCOUNT_UNAVAILABLE));
            } catch (PasswordResetException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new PasswordResetException(PasswordResetException.Reason.PERSISTENCE_FAILED);
            }
            String replacement;
            try {
                // Adaptive work holds neither a database transaction nor a target row lock.
                replacement = encoder.encode(CharBuffer.wrap(newPassword));
                if (replacement == null || replacement.isBlank()) {
                    throw new IllegalStateException("Missing credential encoding");
                }
            } catch (RuntimeException failure) {
                throw new PasswordResetException(PasswordResetException.Reason.ENCODING_FAILED);
            }
            try {
                mutation.executeWithoutResult(status -> persist(administrator, target, observed, replacement));
            } catch (PasswordResetException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new PasswordResetException(PasswordResetException.Reason.PERSISTENCE_FAILED);
            }
        } finally {
            InitialCredentials.clear(newPassword);
            InitialCredentials.clear(confirmation);
        }
    }

    private void persist(UUID administrator, UUID target, long observed, String replacement) {
        AccountEntity account = accounts.findByPublicId(target).orElseThrow(() ->
                new PasswordResetException(PasswordResetException.Reason.ACCOUNT_UNAVAILABLE));
        // Refresh a potentially servlet-bound managed snapshot under the held Account lock.
        entities.refresh(account);
        if (account.authenticationGeneration() != observed) {
            throw new PasswordResetException(PasswordResetException.Reason.STALE_ACCOUNT);
        }
        account.changePassword(replacement, clock.instant().truncatedTo(ChronoUnit.MICROS));
        audit.record(new AuditRequest(administrator.toString(), "IDENTITY_PASSWORD_RESET",
                "IDENTITY_ACCOUNT", target.toString(), null, "administrative-password-reset"));
        accounts.flush();
        // Revocation failure rolls back writes. Later commit failure may retain expired
        // sessions with the old database credential: the existing safe non-atomic asymmetry.
        try {
            sessions.revoke(target);
        } catch (RuntimeException failure) {
            throw new PasswordResetException(PasswordResetException.Reason.SESSION_REVOCATION_FAILED);
        }
    }
}

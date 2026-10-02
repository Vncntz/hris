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

/** Self-service credential command; CurrentActor is the sole actor/target source. */
@Service
public class PasswordChangeService {
    private final CurrentActor actor;
    private final AccountRepository accounts;
    private final EntityManager entities;
    private final PasswordEncoder encoder;
    private final AuditRecorder audit;
    private final AuthenticatedSessionRevoker sessions;
    private final Clock clock;
    private final TransactionTemplate mutation;

    PasswordChangeService(CurrentActor actor, AccountRepository accounts, EntityManager entities, PasswordEncoder encoder,
            AuditRecorder audit, AuthenticatedSessionRevoker sessions, Clock clock,
            PlatformTransactionManager transactions) {
        this.actor = actor;
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

    /** Takes ownership of every supplied buffer, even on authentication denial. */
    public void change(char[] currentPassword, char[] newPassword, char[] confirmation) {
        try {
            UUID id = actor.requireUserId();
            InitialCredentials.validate(newPassword, confirmation);
            if (currentPassword == null) {
                throw new PasswordChangeException(PasswordChangeException.Reason.CURRENT_CREDENTIAL_REJECTED);
            }
            // Refuse caller-held transactions/locks before either adaptive credential operation.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new PasswordChangeException(PasswordChangeException.Reason.PERSISTENCE_FAILED);
            }
            String verified;
            try {
                verified = accounts.findEnabledCredential(id).orElseThrow(() ->
                        new PasswordChangeException(PasswordChangeException.Reason.ACCOUNT_UNAVAILABLE));
                if (!encoder.matches(CharBuffer.wrap(currentPassword), verified)) {
                    throw new PasswordChangeException(PasswordChangeException.Reason.CURRENT_CREDENTIAL_REJECTED);
                }
            } catch (PasswordChangeException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new PasswordChangeException(PasswordChangeException.Reason.PERSISTENCE_FAILED);
            }
            String replacement;
            try {
                replacement = encoder.encode(CharBuffer.wrap(newPassword));
                if (replacement == null || replacement.isBlank()) {
                    throw new IllegalStateException("Missing credential encoding");
                }
            } catch (RuntimeException failure) {
                throw new PasswordChangeException(PasswordChangeException.Reason.ENCODING_FAILED);
            }
            try {
                mutation.executeWithoutResult(status -> persist(id, verified, replacement));
            } catch (PasswordChangeException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new PasswordChangeException(PasswordChangeException.Reason.PERSISTENCE_FAILED);
            }
        } finally {
            InitialCredentials.clear(currentPassword);
            InitialCredentials.clear(newPassword);
            InitialCredentials.clear(confirmation);
        }
    }

    private void persist(UUID id, String verified, String replacement) {
        AccountEntity account = accounts.findByPublicId(id).orElseThrow(() ->
                new PasswordChangeException(PasswordChangeException.Reason.ACCOUNT_UNAVAILABLE));
        // A servlet-bound persistence context can outlive a transaction. The locking query
        // may return its already-managed entity; reload under the held lock before rechecks.
        entities.refresh(account);
        if (!account.enabled()) {
            throw new PasswordChangeException(PasswordChangeException.Reason.ACCOUNT_UNAVAILABLE);
        }
        if (!verified.equals(account.passwordHash())) {
            throw new PasswordChangeException(PasswordChangeException.Reason.STALE_CREDENTIAL);
        }
        account.changePassword(replacement, clock.instant().truncatedTo(ChronoUnit.MICROS));
        audit.record(new AuditRequest(id.toString(), "IDENTITY_PASSWORD_CHANGED",
                "IDENTITY_ACCOUNT", id.toString(), null, "self-service-password-change"));
        // Flush the shared persistence context (account AND audit) before local expiry marking.
        accounts.flush();
        // A marking failure rolls back the database. A later commit failure may leave sessions
        // expired with the old credential still valid: deliberately safe, non-atomic asymmetry.
        try {
            sessions.revoke(id);
        } catch (RuntimeException failure) {
            throw new PasswordChangeException(PasswordChangeException.Reason.SESSION_REVOCATION_FAILED);
        }
    }
}

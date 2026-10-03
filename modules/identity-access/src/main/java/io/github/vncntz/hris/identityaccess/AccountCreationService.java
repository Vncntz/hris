package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.sql.SQLException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Authenticated command boundary; creates no role or permission assignments. */
@Service
public class AccountCreationService {
    private final CurrentActor actor;
    private final RecentAuthenticationGuard recent;
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final AuditRecorder audit;
    private final Clock clock;
    private final TransactionTemplate transaction;

    AccountCreationService(CurrentActor actor, RecentAuthenticationGuard recent, AccountRepository accounts, PasswordEncoder encoder,
            AuditRecorder audit, Clock clock, PlatformTransactionManager transactions) {
        this.actor = actor;
        this.recent = recent;
        this.accounts = accounts;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
        transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(15);
    }

    /** Takes ownership of both buffers, including when authorization fails. */
    public CreatedAccount create(String login, char[] password, char[] confirmation) {
        try {
            actor.requireAuthority("identity:admin");
            recent.requireRecentAuthentication();
            UUID creator = actor.requireUserId();
            String canonical = LoginNames.canonicalize(login);
            InitialCredentials.validate(password, confirmation);
            // This standalone command must not hash while a caller holds a database transaction.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new AccountCreationException(AccountCreationException.Reason.PERSISTENCE_FAILED);
            }
            String hash;
            try {
                hash = encoder.encode(CharBuffer.wrap(password));
            } catch (RuntimeException failure) {
                throw new AccountCreationException(AccountCreationException.Reason.ENCODING_FAILED);
            }
            try {
                return transaction.execute(status -> persist(creator, canonical, hash));
            } catch (RuntimeException failure) {
                throw new AccountCreationException(isDuplicateLogin(failure)
                        ? AccountCreationException.Reason.DUPLICATE_LOGIN
                        : AccountCreationException.Reason.PERSISTENCE_FAILED);
            }
        } finally {
            InitialCredentials.clear(password);
            InitialCredentials.clear(confirmation);
        }
    }

    private CreatedAccount persist(UUID creator, String login, String hash) {
        UUID id = UUID.randomUUID();
        accounts.saveAndFlush(new AccountEntity(id, login, hash,
                clock.instant().truncatedTo(ChronoUnit.MICROS)));
        audit.record(new AuditRequest(creator.toString(), "IDENTITY_ACCOUNT_CREATED",
                "IDENTITY_ACCOUNT", id.toString(), null, "authenticated-account-creation"));
        return new CreatedAccount(id);
    }

    private static boolean isDuplicateLogin(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() == 1062
                    && sql.getMessage() != null
                    && sql.getMessage().contains("uq_identity_account_canonical_login")) {
                return true;
            }
        }
        return false;
    }
}

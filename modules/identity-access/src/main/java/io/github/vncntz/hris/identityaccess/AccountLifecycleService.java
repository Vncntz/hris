package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Administrative lifecycle command; public UUID is the sole target selector. */
@Service
public class AccountLifecycleService {
    private final CurrentActor actor;
    private final RecentAuthenticationGuard recent;
    private final AccountRepository accounts;
    private final EntityManager entities;
    private final AuditRecorder audit;
    private final AuthenticatedSessionRevoker sessions;
    private final Clock clock;
    private final TransactionTemplate mutation;

    AccountLifecycleService(CurrentActor actor, RecentAuthenticationGuard recent, AccountRepository accounts, EntityManager entities,
            AuditRecorder audit, AuthenticatedSessionRevoker sessions, Clock clock,
            PlatformTransactionManager transactions) {
        this.actor = actor;
        this.recent = recent;
        this.accounts = accounts;
        this.entities = entities;
        this.audit = audit;
        this.sessions = sessions;
        this.clock = clock;
        mutation = new TransactionTemplate(transactions);
        mutation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        mutation.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        mutation.setTimeout(15);
    }

    public void enable(UUID target) {
        change(target, true);
    }

    public void disable(UUID target) {
        change(target, false);
    }

    private void change(UUID target, boolean enabled) {
        actor.requireAuthority("identity:admin");
        recent.requireRecentAuthentication();
        UUID administrator = actor.requireUserId();
        if (target == null) {
            throw new AccountLifecycleException(AccountLifecycleException.Reason.INVALID_TARGET);
        }
        if (!enabled && administrator.equals(target)) {
            throw new AccountLifecycleException(AccountLifecycleException.Reason.SELF_DISABLE_REJECTED);
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new AccountLifecycleException(AccountLifecycleException.Reason.PERSISTENCE_FAILED);
        }
        try {
            mutation.executeWithoutResult(status -> persist(administrator, target, enabled));
        } catch (AccountLifecycleException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new AccountLifecycleException(AccountLifecycleException.Reason.PERSISTENCE_FAILED);
        }
    }

    private void persist(UUID administrator, UUID target, boolean enabled) {
        AccountEntity account = accounts.findByPublicId(target).orElseThrow(() ->
                new AccountLifecycleException(AccountLifecycleException.Reason.ACCOUNT_UNAVAILABLE));
        // The locking query can return an entity retained by a servlet persistence context.
        entities.refresh(account);
        if (account.enabled() == enabled) {
            throw new AccountLifecycleException(enabled ? AccountLifecycleException.Reason.ALREADY_ENABLED
                    : AccountLifecycleException.Reason.ALREADY_DISABLED);
        }
        account.changeEnabled(enabled, clock.instant().truncatedTo(ChronoUnit.MICROS));
        audit.record(new AuditRequest(administrator.toString(),
                enabled ? "IDENTITY_ACCOUNT_ENABLED" : "IDENTITY_ACCOUNT_DISABLED",
                "IDENTITY_ACCOUNT", target.toString(), null, "administrative-account-lifecycle"));
        accounts.flush();
        // Expiry failure rolls back both writes. A later commit failure can leave sessions
        // expired while the database retains its preceding state: safe non-atomic asymmetry.
        try {
            sessions.revoke(target);
        } catch (RuntimeException failure) {
            throw new AccountLifecycleException(AccountLifecycleException.Reason.SESSION_REVOCATION_FAILED);
        }
    }
}

package io.github.vncntz.hris.identityaccess;

import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Account-to-Role membership command; only stable public UUIDs cross the boundary. */
@Service
public class AccountRoleAssignmentService {
    private final CurrentActor actor;
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final EntityManager entities;
    private final AuditRecorder audit;
    private final AuthenticatedSessionRevoker sessions;
    private final TransactionTemplate mutation;

    AccountRoleAssignmentService(CurrentActor actor, AccountRepository accounts, RoleRepository roles,
            EntityManager entities, AuditRecorder audit, AuthenticatedSessionRevoker sessions,
            PlatformTransactionManager transactions) {
        this.actor = actor;
        this.accounts = accounts;
        this.roles = roles;
        this.entities = entities;
        this.audit = audit;
        this.sessions = sessions;
        mutation = new TransactionTemplate(transactions);
        mutation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        mutation.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        mutation.setTimeout(15);
    }

    public void assign(UUID accountPublicId, UUID rolePublicId) {
        change(accountPublicId, rolePublicId, true);
    }

    public void remove(UUID accountPublicId, UUID rolePublicId) {
        change(accountPublicId, rolePublicId, false);
    }

    private void change(UUID target, UUID roleId, boolean assigned) {
        actor.requireAuthority("identity:admin");
        UUID administrator = actor.requireUserId();
        if (target == null || roleId == null) {
            throw failed(AccountRoleAssignmentException.Reason.INVALID_TARGET);
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw failed(AccountRoleAssignmentException.Reason.PERSISTENCE_FAILED);
        }
        try {
            mutation.executeWithoutResult(status -> persist(administrator, target, roleId, assigned));
        } catch (AccountRoleAssignmentException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw failed(AccountRoleAssignmentException.Reason.PERSISTENCE_FAILED);
        }
    }

    private void persist(UUID administrator, UUID target, UUID roleId, boolean assigned) {
        AccountEntity account = accounts.findByPublicId(target).orElseThrow(() ->
                failed(AccountRoleAssignmentException.Reason.ACCOUNT_UNAVAILABLE));
        // A servlet persistence context may outlive a transaction; reload under the held lock.
        entities.refresh(account);
        RoleEntity role = roles.findByPublicId(roleId).orElseThrow(() ->
                failed(AccountRoleAssignmentException.Reason.ROLE_UNAVAILABLE));
        entities.refresh(role);
        boolean present = account.assignedRoles().stream().anyMatch(value -> roleId.equals(value.publicId()));
        if (present == assigned) {
            throw failed(assigned ? AccountRoleAssignmentException.Reason.ALREADY_ASSIGNED
                    : AccountRoleAssignmentException.Reason.NOT_ASSIGNED);
        }
        if (assigned && !role.enabled()) {
            throw failed(AccountRoleAssignmentException.Reason.ROLE_DISABLED);
        }
        if (!assigned && administrator.equals(target)) {
            // Refresh remaining managed Roles as well: effective authority is persisted enabled membership,
            // never the caller's potentially stale token snapshot or a canonical Role-name shortcut.
            boolean remainsAdmin = false;
            for (RoleEntity remaining : account.assignedRoles()) {
                if (!roleId.equals(remaining.publicId())) {
                    entities.refresh(remaining);
                    if (remaining.enabled() && remaining.authorityKeys().contains("identity:admin")) {
                        remainsAdmin = true;
                    }
                }
            }
            if (!remainsAdmin) {
                throw failed(AccountRoleAssignmentException.Reason.SELF_ADMIN_REMOVAL_REJECTED);
            }
        }
        account.changeRoleAssignment(role, assigned);
        audit.record(new AuditRequest(administrator.toString(),
                assigned ? "IDENTITY_ACCOUNT_ROLE_ASSIGNED" : "IDENTITY_ACCOUNT_ROLE_REMOVED",
                "IDENTITY_ACCOUNT_ROLE", target + "/" + roleId, null,
                "administrative-account-role-assignment"));
        accounts.flush();
        // Expiry failure rolls back membership/generation/audit. A later commit failure may leave
        // sessions expired with the preceding database state retained: safe non-atomic asymmetry.
        try {
            sessions.revoke(target);
        } catch (RuntimeException failure) {
            throw failed(AccountRoleAssignmentException.Reason.SESSION_REVOCATION_FAILED);
        }
    }

    private static AccountRoleAssignmentException failed(AccountRoleAssignmentException.Reason reason) {
        return new AccountRoleAssignmentException(reason);
    }
}

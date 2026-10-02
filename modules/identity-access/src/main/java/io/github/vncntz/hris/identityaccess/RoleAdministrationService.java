package io.github.vncntz.hris.identityaccess;

import java.util.UUID;
import java.util.function.Supplier;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static io.github.vncntz.hris.identityaccess.RoleAdministrationException.Reason.*;

/** Authenticated Role/Permission commands. Persistence and relational IDs remain internal. */
@Service
public class RoleAdministrationService {
    private final CurrentActor actor;
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final EntityManager entities;
    private final AuditRecorder audit;
    private final AuthenticatedSessionRevoker sessions;
    private final TransactionTemplate mutation;

    RoleAdministrationService(CurrentActor actor, AccountRepository accounts, RoleRepository roles,
            PermissionRepository permissions, EntityManager entities, AuditRecorder audit,
            AuthenticatedSessionRevoker sessions, PlatformTransactionManager transactions) {
        this.actor = actor;
        this.accounts = accounts;
        this.roles = roles;
        this.permissions = permissions;
        this.entities = entities;
        this.audit = audit;
        this.sessions = sessions;
        mutation = new TransactionTemplate(transactions);
        mutation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        mutation.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        mutation.setTimeout(15);
    }

    public UUID createRole(String name) {
        UUID administrator = authorize();
        RoleEntity role;
        try {
            role = new RoleEntity(UUID.randomUUID(), name, false);
        } catch (IllegalArgumentException failure) {
            throw failed(INVALID_INPUT);
        }
        return execute(() -> {
            if (roles.existsByCanonicalName(role.canonicalName())) {
                throw failed(DUPLICATE_ROLE);
            }
            roles.save(role);
            record(administrator, "IDENTITY_ROLE_CREATED", "IDENTITY_ROLE", role.publicId().toString(),
                    "administrative-role-management");
            entities.flush();
            return role.publicId();
        });
    }

    public String createPermission(String authorityKey) {
        UUID administrator = authorize();
        PermissionEntity permission = canonicalPermission(authorityKey);
        return execute(() -> {
            if (permissions.findByAuthorityKey(permission.authorityKey()).isPresent()) {
                throw failed(DUPLICATE_PERMISSION);
            }
            permissions.save(permission);
            record(administrator, "IDENTITY_PERMISSION_CREATED", "IDENTITY_PERMISSION", permission.authorityKey(),
                    "administrative-permission-management");
            entities.flush();
            return permission.authorityKey();
        });
    }

    public void enable(UUID roleId) {
        change(roleId, null, true, false);
    }

    public void disable(UUID roleId) {
        change(roleId, null, false, false);
    }

    public void assignPermission(UUID roleId, String authorityKey) {
        change(roleId, authorityKey, true, true);
    }

    public void removePermission(UUID roleId, String authorityKey) {
        change(roleId, authorityKey, false, true);
    }

    private void change(UUID roleId, String suppliedKey, boolean added, boolean membership) {
        UUID administrator = authorize();
        String key = membership ? canonicalPermission(suppliedKey).authorityKey() : null;
        if (roleId == null) {
            throw failed(INVALID_INPUT);
        }
        execute(() -> {
            // Never lock a Role then an Account. The actor Account stabilizes self membership;
            // ordered actor Roles stabilize persisted self-admin checks against concurrent changes.
            AccountEntity account = accounts.findByPublicId(administrator).orElseThrow(() -> failed(PERSISTENCE_FAILED));
            entities.refresh(account);
            var roleIds = new java.util.TreeSet<UUID>();
            account.assignedRoles().forEach(role -> roleIds.add(role.publicId()));
            roleIds.add(roleId);
            var locked = new java.util.HashMap<UUID, RoleEntity>();
            for (UUID id : roleIds) {
                RoleEntity role = roles.findByPublicId(id).orElseThrow(() -> failed(ROLE_UNAVAILABLE));
                entities.refresh(role);
                locked.put(id, role);
            }
            RoleEntity role = locked.get(roleId);
            PermissionEntity permission = key == null ? null : permissions.findByAuthorityKey(key)
                    .orElseThrow(() -> failed(PERMISSION_UNAVAILABLE));
            boolean present = key == null ? role.enabled() : role.authorityKeys().contains(key);
            if (present == added) {
                throw failed(key == null ? (added ? ALREADY_ENABLED : ALREADY_DISABLED)
                        : (added ? ALREADY_ASSIGNED : NOT_ASSIGNED));
            }
            if (!added && (key == null || "identity:admin".equals(key))
                    && account.assignedRoles().stream().anyMatch(value -> roleId.equals(value.publicId()))) {
                boolean remainsAdmin = account.assignedRoles().stream().map(value -> locked.get(value.publicId()))
                        .anyMatch(value -> !roleId.equals(value.publicId()) && value.enabled()
                                && value.authorityKeys().contains("identity:admin"));
                if (!remainsAdmin) {
                    throw failed(SELF_ADMIN_REMOVAL_REJECTED);
                }
            }
            if (permission == null) {
                role.changeEnabled(added);
            } else {
                role.changePermission(permission, added);
            }
            record(administrator, key == null ? (added ? "IDENTITY_ROLE_ENABLED" : "IDENTITY_ROLE_DISABLED")
                    : (added ? "IDENTITY_ROLE_PERMISSION_ASSIGNED" : "IDENTITY_ROLE_PERMISSION_REMOVED"),
                    key == null ? "IDENTITY_ROLE" : "IDENTITY_ROLE_PERMISSION", roleId.toString(),
                    key == null ? "administrative-role-management" : "administrative-role-permission-assignment/" + key);
            entities.flush();
            var affected = accounts.findPublicIdsAssignedToRole(roleId);
            try {
                sessions.revoke(affected);
            } catch (RuntimeException failure) {
                throw failed(SESSION_REVOCATION_FAILED);
            }
            // Expiry precedes commit. A late commit failure safely retains expired sessions
            // while rolling back Role/generation/audit; fresh authentication reads retained state.
            return null;
        });
    }

    private UUID authorize() {
        actor.requireAuthority("identity:admin");
        return actor.requireUserId();
    }

    private <T> T execute(Supplier<T> command) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw failed(PERSISTENCE_FAILED);
        }
        try {
            return mutation.execute(status -> command.get());
        } catch (RoleAdministrationException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw failed(PERSISTENCE_FAILED);
        }
    }

    private void record(UUID administrator, String action, String type, String reference, String context) {
        audit.record(new AuditRequest(administrator.toString(), action, type, reference, null, context));
    }

    private static PermissionEntity canonicalPermission(String key) {
        try {
            return new PermissionEntity(key);
        } catch (IllegalArgumentException failure) {
            throw failed(INVALID_INPUT);
        }
    }

    private static RoleAdministrationException failed(RoleAdministrationException.Reason reason) {
        return new RoleAdministrationException(reason);
    }
}

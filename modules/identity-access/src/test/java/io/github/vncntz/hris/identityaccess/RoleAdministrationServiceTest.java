package io.github.vncntz.hris.identityaccess;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static io.github.vncntz.hris.identityaccess.RoleAdministrationException.Reason.*;

class RoleAdministrationServiceTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PermissionRepository permissions = mock(PermissionRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final AuthenticatedSessionRevoker sessions = mock(AuthenticatedSessionRevoker.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID administrator = UUID.randomUUID();
    private final AccountEntity account = new AccountEntity(administrator, "synthetic.actor",
            UUID.randomUUID().toString(), Instant.EPOCH);
    private final RoleEntity role = new RoleEntity(UUID.randomUUID(), "synthetic.role", false);
    private final PermissionEntity permission = new PermissionEntity("test:read");
    private final Set<UUID> affected = Set.of(UUID.randomUUID(), UUID.randomUUID());
    private final RoleAdministrationService service = new RoleAdministrationService(actor, accounts, roles,
            permissions, entities, audit, sessions, transactions);

    private void ready() {
        when(actor.requireUserId()).thenReturn(administrator);
        when(accounts.findByPublicId(administrator)).thenReturn(Optional.of(account));
        when(roles.findByPublicId(any())).thenAnswer(call -> {
            UUID id = call.getArgument(0);
            return id.equals(role.publicId()) ? Optional.of(role)
                    : account.assignedRoles().stream().filter(value -> id.equals(value.publicId())).findFirst();
        });
        when(permissions.findByAuthorityKey(permission.authorityKey())).thenReturn(Optional.of(permission));
        when(accounts.findPublicIdsAssignedToRole(role.publicId())).thenReturn(affected);
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            TransactionDefinition definition = call.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
    }

    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }

    @Test void allSixCommandsRequireAuthorityFirstAndDeriveActorOnlyFromCurrentActor() {
        doThrow(new AccessDeniedException("Denied")).when(actor).requireAuthority("identity:admin");
        for (Runnable command : commands()) { assertThrows(AccessDeniedException.class, command::run); }
        verify(actor, times(6)).requireAuthority("identity:admin");
        verifyNoMoreInteractions(actor);
        verifyNoInteractions(accounts, roles, permissions, entities, audit, sessions, transactions);
    }

    private List<Runnable> commands() {
        return List.of(() -> service.createRole("role"), () -> service.createPermission("test:new"),
                () -> service.enable(role.publicId()), () -> service.disable(role.publicId()),
                () -> service.assignPermission(role.publicId(), "test:read"),
                () -> service.removePermission(role.publicId(), "test:read"));
    }

    @Test void creationsAreCanonicalDisabledEmptyZeroAndNeverInvalidateExistingAccounts() {
        ready();
        UUID id = service.createRole(" Synthetic.New ");
        var created = org.mockito.ArgumentCaptor.forClass(RoleEntity.class);
        verify(roles).save(created.capture());
        assertEquals(id, created.getValue().publicId());
        assertEquals("synthetic.new", created.getValue().canonicalName());
        assertFalse(created.getValue().enabled());
        assertTrue(created.getValue().authorityKeys().isEmpty());
        assertEquals(0, created.getValue().authorizationGeneration());
        assertEquals("test:new", service.createPermission(" TEST:NEW "));
        verify(audit).record(event("IDENTITY_ROLE_CREATED", "IDENTITY_ROLE", id.toString(), "administrative-role-management"));
        verify(audit).record(event("IDENTITY_PERMISSION_CREATED", "IDENTITY_PERMISSION", "test:new", "administrative-permission-management"));
        verifyNoInteractions(accounts, sessions);
        verify(actor, times(2)).requireUserId();
    }

    @Test void invalidCanonicalInputsNullTargetsAndAmbientTransactionsAreBounded() {
        ready();
        for (String invalid : java.util.Arrays.asList(null, "", "a b", "é", "a".repeat(129))) {
            bounded(INVALID_INPUT, () -> service.createRole(invalid));
            bounded(INVALID_INPUT, () -> service.createPermission(invalid));
        }
        bounded(INVALID_INPUT, () -> service.enable(null));
        bounded(INVALID_INPUT, () -> service.assignPermission(role.publicId(), null));
        TransactionSynchronizationManager.setActualTransactionActive(true);
        for (Runnable command : commands()) { bounded(PERSISTENCE_FAILED, command); }
        verifyNoInteractions(roles, permissions, accounts, entities, audit, sessions, transactions);
    }

    @Test void duplicatesMissingTargetsAndNoOpsNeverAdvanceAuditOrExpire() {
        ready();
        when(roles.existsByCanonicalName("duplicate")).thenReturn(true);
        bounded(DUPLICATE_ROLE, () -> service.createRole("duplicate"));
        bounded(DUPLICATE_PERMISSION, () -> service.createPermission("test:read"));
        bounded(ROLE_UNAVAILABLE, () -> service.enable(UUID.randomUUID()));
        bounded(PERMISSION_UNAVAILABLE, () -> service.assignPermission(role.publicId(), "test:missing"));
        bounded(ALREADY_DISABLED, () -> service.disable(role.publicId()));
        bounded(NOT_ASSIGNED, () -> service.removePermission(role.publicId(), "test:read"));
        role.assignBootstrapPermission(permission);
        bounded(ALREADY_ASSIGNED, () -> service.assignPermission(role.publicId(), "test:read"));
        ReflectionTestUtils.setField(role, "enabled", true);
        bounded(ALREADY_ENABLED, () -> service.enable(role.publicId()));
        assertEquals(0, role.authorizationGeneration());
        verifyNoInteractions(audit, sessions);
        verify(transactions, times(8)).rollback(any());
    }

    @Test void mutationsAdvanceExactlyOncePreserveIdentityAndFlushBeforeOneSetRevocation() {
        ready();
        for (Runnable command : List.<Runnable>of(() -> service.assignPermission(role.publicId(), "test:read"),
                () -> service.enable(role.publicId()), () -> service.removePermission(role.publicId(), "test:read"),
                () -> service.disable(role.publicId()))) {
            long prior = role.authorizationGeneration();
            command.run();
            assertEquals(prior + 1, role.authorizationGeneration());
            assertEquals("synthetic.role", role.canonicalName());
            assertEquals(0, account.authenticationGeneration());
        }
        var order = inOrder(actor, transactions, accounts, roles, entities, audit, sessions);
        for (String action : List.of("IDENTITY_ROLE_PERMISSION_ASSIGNED", "IDENTITY_ROLE_ENABLED",
                "IDENTITY_ROLE_PERMISSION_REMOVED", "IDENTITY_ROLE_DISABLED")) {
            order.verify(actor).requireAuthority("identity:admin");
            order.verify(actor).requireUserId();
            order.verify(transactions).getTransaction(any());
            order.verify(accounts).findByPublicId(administrator);
            order.verify(entities).refresh(account);
            order.verify(roles).findByPublicId(role.publicId());
            order.verify(entities).refresh(role);
            order.verify(audit).record(event(action, action.contains("PERMISSION") ? "IDENTITY_ROLE_PERMISSION" : "IDENTITY_ROLE",
                    role.publicId().toString(), action.contains("PERMISSION")
                    ? "administrative-role-permission-assignment/test:read" : "administrative-role-management"));
            order.verify(entities).flush();
            order.verify(accounts).findPublicIdsAssignedToRole(role.publicId());
            order.verify(sessions).revoke(affected);
            order.verify(transactions).commit(any());
        }
        assertTrue(role.authorityKeys().isEmpty());
        assertFalse(role.enabled());
    }

    @Test void maximumCanonicalKeyAuditsIntactInContextAndOnlyUuidInTarget() {
        ready();
        String key = "a".repeat(128);
        when(permissions.findByAuthorityKey(key)).thenReturn(Optional.of(new PermissionEntity(key)));
        service.assignPermission(role.publicId(), key);
        assertFalse(role.enabled());
        verify(audit).record(event("IDENTITY_ROLE_PERMISSION_ASSIGNED", "IDENTITY_ROLE_PERMISSION",
                role.publicId().toString(), "administrative-role-permission-assignment/" + key));
    }

    @Test void selfAdminChecksPersistedEnabledRolesRatherThanNameOrToken() {
        ready();
        ReflectionTestUtils.setField(role, "enabled", true);
        PermissionEntity admin = new PermissionEntity("identity:admin");
        role.assignBootstrapPermission(admin);
        account.assignBootstrapRole(role);
        when(permissions.findByAuthorityKey("identity:admin")).thenReturn(Optional.of(admin));
        RoleEntity disabled = new RoleEntity(UUID.randomUUID(), "administrator", false);
        disabled.assignBootstrapPermission(admin);
        account.assignBootstrapRole(disabled);
        bounded(SELF_ADMIN_REMOVAL_REJECTED, () -> service.disable(role.publicId()));
        bounded(SELF_ADMIN_REMOVAL_REJECTED, () -> service.removePermission(role.publicId(), "identity:admin"));
        assertEquals(0, role.authorizationGeneration());
        verifyNoInteractions(audit, sessions);
        disabled.changeEnabled(true);
        service.removePermission(role.publicId(), "identity:admin");
        assertEquals(1, role.authorizationGeneration());
    }

    @Test void locksActorAccountThenAllActorRolesAndTargetInDeterministicOrder() {
        ready();
        RoleEntity first = new RoleEntity(new UUID(0, 1), "first", true);
        RoleEntity last = new RoleEntity(new UUID(0, 9), "last", true);
        account.assignBootstrapRole(last);
        account.assignBootstrapRole(first);
        service.enable(role.publicId());
        var order = inOrder(accounts, roles, entities);
        order.verify(accounts).findByPublicId(administrator);
        order.verify(entities).refresh(account);
        for (UUID id : List.of(first.publicId(), last.publicId(), role.publicId()).stream().sorted().toList()) {
            order.verify(roles).findByPublicId(id);
            order.verify(entities).refresh(id.equals(role.publicId()) ? role : id.equals(first.publicId()) ? first : last);
        }
    }

    @Test void overflowNeverChangesRoleStateOrMembership() {
        ready();
        ReflectionTestUtils.setField(role, "authorizationGeneration", Long.MAX_VALUE);
        bounded(PERSISTENCE_FAILED, () -> service.enable(role.publicId()));
        bounded(PERSISTENCE_FAILED, () -> service.assignPermission(role.publicId(), "test:read"));
        assertEquals(Long.MAX_VALUE, role.authorizationGeneration());
        assertFalse(role.enabled());
        assertTrue(role.authorityKeys().isEmpty());
        verifyNoInteractions(audit, sessions);
    }

    @Test void auditFlushDiscoveryAndExpiryFailuresRollbackWithBoundedCauses() {
        for (int scenario = 0; scenario < 4; scenario++) {
            reset(audit, entities, accounts, roles, sessions, transactions);
            ready();
            ReflectionTestUtils.setField(role, "enabled", false);
            if (scenario == 0) { doThrow(new IllegalStateException("Synthetic audit failure")).when(audit).record(any()); }
            if (scenario == 1) { doThrow(new IllegalStateException("Synthetic flush failure")).when(entities).flush(); }
            if (scenario == 2) { when(accounts.findPublicIdsAssignedToRole(any())).thenThrow(new IllegalStateException("Synthetic discovery failure")); }
            if (scenario == 3) { doThrow(new IllegalStateException("Synthetic expiry failure")).when(sessions).revoke(affected); }
            bounded(scenario == 3 ? SESSION_REVOCATION_FAILED : PERSISTENCE_FAILED, () -> service.enable(role.publicId()));
            verify(transactions).rollback(any());
            if (scenario < 3) { verifyNoInteractions(sessions); }
        }
    }

    @Test void lateCommitFailureFollowsSuccessfulExpiryAndExposesOnlyBoundedOutcome() {
        ready();
        doThrow(new IllegalStateException("Synthetic commit failure")).when(transactions).commit(any());
        bounded(PERSISTENCE_FAILED, () -> service.enable(role.publicId()));
        var order = inOrder(entities, sessions, transactions);
        order.verify(entities).flush();
        order.verify(sessions).revoke(affected);
        order.verify(transactions).commit(any());
        // Actual database rollback/asymmetry is verified by MySQL integration tests.
    }

    private AuditRequest event(String action, String type, String reference, String context) {
        return new AuditRequest(administrator.toString(), action, type, reference, null, context);
    }

    private static void bounded(RoleAdministrationException.Reason reason, Runnable command) {
        var failure = assertThrows(RoleAdministrationException.class, command::run);
        assertEquals(reason, failure.reason());
        assertEquals("Role administration failed: " + reason, failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }
}

package io.github.vncntz.hris.identityaccess;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static io.github.vncntz.hris.identityaccess.AccountRoleAssignmentException.Reason.*;

class AccountRoleAssignmentServiceTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final RecentAuthenticationGuard recent = mock(RecentAuthenticationGuard.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final AuthenticatedSessionRevoker sessions = mock(AuthenticatedSessionRevoker.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID administrator = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final RoleEntity role = role(true, "identity:admin");
    private final AccountEntity account = new AccountEntity(target, "synthetic.target", UUID.randomUUID().toString(), Instant.EPOCH);
    private final AccountRoleAssignmentService service = service(actor);

    private AccountRoleAssignmentService service(CurrentActor current) {
        return new AccountRoleAssignmentService(current, recent, accounts, roles, entities, audit, sessions, transactions);
    }

    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
    }

    private void ready() {
        when(actor.requireUserId()).thenReturn(administrator);
        when(accounts.findByPublicId(target)).thenReturn(Optional.of(account));
        when(roles.findByPublicId(any())).thenAnswer(call -> {
            UUID requested = call.getArgument(0);
            if (role.publicId().equals(requested)) { return Optional.of(role); }
            return account.assignedRoles().stream().filter(value -> requested.equals(value.publicId())).findFirst();
        });
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            TransactionDefinition definition = call.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
    }

    @Test void assignRemovePreservesEveryUnrelatedFieldAndOrdersAuditFlushExpiryCommit() throws Exception {
        ready();
        RoleEntity unrelated = role(true, "test:read");
        account.assignBootstrapRole(unrelated);
        account.recordFailure(Instant.EPOCH, 1, Duration.ofDays(1));
        ReflectionTestUtils.setField(account, "enabled", false);
        var fields = List.of("publicId", "canonicalLogin", "passwordHash", "enabled", "failedAttempts",
                "lockedUntilUtc", "credentialUpdatedAtUtc", "securityUpdatedAtUtc", "rowVersion");
        var before = fields.stream().map(field -> ReflectionTestUtils.getField(account, field)).toList();
        for (boolean assigned : List.of(true, false)) {
            if (assigned) { service.assign(target, role.publicId()); } else { service.remove(target, role.publicId()); }
            assertEquals(assigned ? 1 : 2, account.authenticationGeneration());
            assertEquals(assigned ? java.util.Set.of(unrelated, role) : java.util.Set.of(unrelated), account.assignedRoles());
            // Boolean avoids printing credential state on a failed assertion.
            assertTrue(before.equals(fields.stream().map(field -> ReflectionTestUtils.getField(account, field)).toList()));
            assertTrue(role.enabled());
            assertEquals(List.of("identity:admin"), role.authorityKeys());
        }
        var order = inOrder(actor, transactions, accounts, roles, entities, audit, sessions);
        for (String action : List.of("IDENTITY_ACCOUNT_ROLE_ASSIGNED", "IDENTITY_ACCOUNT_ROLE_REMOVED")) {
            order.verify(actor).requireAuthority("identity:admin");
            order.verify(actor).requireUserId();
            order.verify(transactions).getTransaction(any());
            order.verify(accounts).findByPublicId(target);
            order.verify(entities).refresh(account);
            for (UUID locked : java.util.stream.Stream.of(role.publicId(), unrelated.publicId()).sorted().toList()) {
                order.verify(roles).findByPublicId(locked);
                order.verify(entities).refresh(locked.equals(role.publicId()) ? role : unrelated);
            }
            order.verify(audit).record(event(action));
            order.verify(accounts).flush();
            order.verify(sessions).revoke(target);
            order.verify(transactions).commit(any());
        }
        verifyNoMoreInteractions(actor, transactions, accounts, roles, entities, audit, sessions);
        for (String method : List.of("assign", "remove")) {
            assertEquals(2, AccountRoleAssignmentService.class.getMethod(method, UUID.class, UUID.class).getParameterCount());
        }
    }

    @Test void realActorRejectsAnonymousUnsupportedUnauthenticatedAndUnauthorizedBeforePersistence() {
        var authorities = List.of(new SimpleGrantedAuthority("identity:admin"));
        for (var token : Arrays.asList(null,
                new AnonymousAuthenticationToken("synthetic", "anonymousUser", authorities),
                UsernamePasswordAuthenticationToken.authenticated("unsupported", null, authorities),
                UsernamePasswordAuthenticationToken.unauthenticated(new AccountPrincipal(administrator, "synthetic.admin"), null))) {
            SecurityContextHolder.getContext().setAuthentication(token);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service(new SecurityCurrentActor()).assign(target, role.publicId()));
        }
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(administrator, "synthetic.admin"), null, List.of()));
        assertThrows(AccessDeniedException.class, () -> service(new SecurityCurrentActor()).remove(target, role.publicId()));
        verifyNoInteractions(accounts, roles, entities, transactions, audit, sessions);
    }

    @Test void realActorDerivesAdministratorAndTargetsOnlyPublicIds() {
        ready();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(administrator, "synthetic.admin"), null, List.of(new SimpleGrantedAuthority("identity:admin"))));
        service(new SecurityCurrentActor()).assign(target, role.publicId());
        verifyNoInteractions(actor);
        verify(accounts).findByPublicId(target);
        verify(roles).findByPublicId(role.publicId());
        verify(audit).record(event("IDENTITY_ACCOUNT_ROLE_ASSIGNED"));
    }

    @Test void invalidTargetsAndAmbientTransactionAreRejectedBeforeMutation() {
        ready();
        bounded(INVALID_TARGET, () -> service.assign(null, role.publicId()));
        bounded(INVALID_TARGET, () -> service.remove(target, null));
        TransactionSynchronizationManager.setActualTransactionActive(true);
        bounded(PERSISTENCE_FAILED, () -> service.assign(target, role.publicId()));
        verifyNoInteractions(accounts, roles, entities, transactions, audit, sessions);
        assertEquals(0, account.authenticationGeneration());
    }

    @Test void missingAccountRoleDuplicateAndMissingMembershipAreNotRetriedAuditedOrRevoked() {
        ready();
        when(accounts.findByPublicId(target)).thenReturn(Optional.empty());
        bounded(ACCOUNT_UNAVAILABLE, () -> service.assign(target, role.publicId()));
        when(accounts.findByPublicId(target)).thenReturn(Optional.of(account));
        when(roles.findByPublicId(role.publicId())).thenReturn(Optional.empty());
        bounded(ROLE_UNAVAILABLE, () -> service.remove(target, role.publicId()));
        when(roles.findByPublicId(role.publicId())).thenReturn(Optional.of(role));
        bounded(NOT_ASSIGNED, () -> service.remove(target, role.publicId()));
        account.assignBootstrapRole(role);
        bounded(ALREADY_ASSIGNED, () -> service.assign(target, role.publicId()));
        assertEquals(0, account.authenticationGeneration());
        verify(transactions, times(4)).rollback(any());
        verifyNoInteractions(audit, sessions);
    }

    @Test void disabledRoleCannotBeAssignedButExistingMembershipCanBeRemoved() {
        ready();
        ReflectionTestUtils.setField(role, "enabled", false);
        bounded(ROLE_DISABLED, () -> service.assign(target, role.publicId()));
        assertEquals(0, account.authenticationGeneration());
        verifyNoInteractions(audit, sessions);
        account.assignBootstrapRole(role);
        service.remove(target, role.publicId());
        assertEquals(1, account.authenticationGeneration());
        assertTrue(account.assignedRoles().isEmpty());
    }

    @Test void selfRemovalRequiresEffectiveAdminInRemainingEnabledRoles() {
        ready();
        when(actor.requireUserId()).thenReturn(target);
        account.assignBootstrapRole(role);
        RoleEntity disabled = role(false, "identity:admin");
        RoleEntity ordinary = role(true, "test:read");
        account.assignBootstrapRole(disabled);
        account.assignBootstrapRole(ordinary);
        bounded(SELF_ADMIN_REMOVAL_REJECTED, () -> service.remove(target, role.publicId()));
        assertEquals(0, account.authenticationGeneration());
        assertTrue(account.assignedRoles().contains(role));
        verifyNoInteractions(audit, sessions);
        RoleEntity alternate = role(true, "identity:admin");
        account.assignBootstrapRole(alternate);
        service.remove(target, role.publicId());
        assertEquals(1, account.authenticationGeneration());
        assertEquals(java.util.Set.of(disabled, ordinary, alternate), account.assignedRoles());
    }

    @Test void harmlessSelfAssignmentAndRemovalAreAllowedAndRevokeSelf() {
        ready();
        when(actor.requireUserId()).thenReturn(target);
        RoleEntity existing = role(true, "identity:admin");
        account.assignBootstrapRole(existing);
        service.assign(target, role.publicId());
        service.remove(target, role.publicId());
        assertEquals(2, account.authenticationGeneration());
        assertEquals(java.util.Set.of(existing), account.assignedRoles());
        verify(sessions, times(2)).revoke(target);
    }

    @Test void managedAccountAndRemainingRolesAreRefreshedBeforeValidation() {
        ready();
        when(actor.requireUserId()).thenReturn(target);
        doAnswer(call -> { account.assignBootstrapRole(role); return null; }).when(entities).refresh(account);
        RoleEntity remaining = role(true, "identity:admin");
        account.assignBootstrapRole(remaining);
        doAnswer(call -> { ReflectionTestUtils.setField(remaining, "enabled", false); return null; }).when(entities).refresh(remaining);
        bounded(SELF_ADMIN_REMOVAL_REJECTED, () -> service.remove(target, role.publicId()));
        assertEquals(0, account.authenticationGeneration());
        verifyNoInteractions(audit, sessions);
    }

    @Test void roleRefreshPreventsStaleEnablementFromGrantingAssignment() {
        ready();
        doAnswer(call -> { ReflectionTestUtils.setField(role, "enabled", false); return null; }).when(entities).refresh(role);
        bounded(ROLE_DISABLED, () -> service.assign(target, role.publicId()));
        assertEquals(0, account.authenticationGeneration());
        verifyNoInteractions(audit, sessions);
    }

    @Test void failuresAreBoundedRollbackAndNeverRevokeBeforeAuditAndFlush() {
        for (int scenario = 0; scenario < 8; scenario++) {
            reset(accounts, roles, entities, transactions, audit, sessions); ready();
            ReflectionTestUtils.setField(account, "roles", new java.util.HashSet<RoleEntity>());
            var internal = new IllegalStateException("Synthetic internal diagnostic");
            switch (scenario) {
                case 0 -> when(accounts.findByPublicId(target)).thenThrow(internal);
                case 1 -> doThrow(internal).when(entities).refresh(account);
                case 2 -> when(roles.findByPublicId(role.publicId())).thenThrow(internal);
                case 3 -> doThrow(internal).when(entities).refresh(role);
                case 4 -> when(audit.record(any())).thenThrow(internal);
                case 5 -> doThrow(internal).when(accounts).flush();
                case 6 -> doThrow(internal).when(sessions).revoke(target);
                case 7 -> doThrow(internal).when(transactions).commit(any());
                default -> fail();
            }
            bounded(scenario == 6 ? SESSION_REVOCATION_FAILED : PERSISTENCE_FAILED, () -> service.assign(target, role.publicId()));
            if (scenario < 6) { verifyNoInteractions(sessions); }
            if (scenario < 7) { verify(transactions).rollback(any()); }
            if (scenario == 7) { verify(sessions).revoke(target); }
        }
    }

    @Test void generationOverflowLeavesMembershipUntouched() {
        ready();
        ReflectionTestUtils.setField(account, "authenticationGeneration", Long.MAX_VALUE);
        bounded(PERSISTENCE_FAILED, () -> service.assign(target, role.publicId()));
        assertTrue(account.assignedRoles().isEmpty());
        assertEquals(Long.MAX_VALUE, account.authenticationGeneration());
        verifyNoInteractions(audit, sessions);
    }

    private AuditRequest event(String action) {
        return new AuditRequest(administrator.toString(), action, "IDENTITY_ACCOUNT_ROLE", target + "/" + role.publicId(),
                null, "administrative-account-role-assignment");
    }
    private static RoleEntity role(boolean enabled, String authority) {
        RoleEntity result = new RoleEntity(UUID.randomUUID(), "synthetic." + UUID.randomUUID(), enabled);
        result.assignBootstrapPermission(new PermissionEntity(authority));
        return result;
    }
    private static void bounded(AccountRoleAssignmentException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(AccountRoleAssignmentException.class, action);
        assertEquals(reason, failure.reason());
        assertEquals("Account role assignment failed: " + reason, failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test void recentProofDenialPrecedesCredentialWorkAndMutation() {
        doThrow(new RecentAuthenticationException(RecentAuthenticationException.Reason.PROOF_REQUIRED))
                .when(recent).requireRecentAuthentication();
        assertThrows(RecentAuthenticationException.class, () -> service.assign(UUID.randomUUID(), UUID.randomUUID()));
        var order = inOrder(actor, recent);
        order.verify(actor).requireAuthority("identity:admin");
        order.verify(recent).requireRecentAuthentication();
        verifyNoInteractions(transactions, accounts, roles, audit, sessions);

    }
}

package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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

class AccountLifecycleServiceTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final RecentAuthenticationGuard recent = mock(RecentAuthenticationGuard.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final AuthenticatedSessionRevoker sessions = mock(AuthenticatedSessionRevoker.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID administrator = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-02T04:05:06.123456Z");
    private final AccountEntity account = new AccountEntity(target, "synthetic.target", UUID.randomUUID().toString(), Instant.EPOCH);
    private final AccountLifecycleService service = service(actor);

    private AccountLifecycleService service(CurrentActor current) {
        return new AccountLifecycleService(current, recent, accounts, entities, audit, sessions,
                Clock.fixed(now, ZoneOffset.UTC), transactions);
    }

    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
    }

    private void ready() {
        when(actor.requireUserId()).thenReturn(administrator);
        when(accounts.findByPublicId(target)).thenReturn(Optional.of(account));
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            TransactionDefinition definition = call.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
    }

    @Test void disableThenEnablePreservesCredentialsIdentityAssignmentsAndOrdersAuditFlushExpiryCommit() throws Exception {
        ready();
        account.assignBootstrapRole(new RoleEntity(UUID.randomUUID(), "synthetic.role", true));
        Object roles = ReflectionTestUtils.getField(account, "roles");
        String credential = account.passwordHash();
        Object timestamp = ReflectionTestUtils.getField(account, "credentialUpdatedAtUtc");
        account.recordFailure(now, 1, Duration.ofMinutes(15));
        service.disable(target);
        assertFalse(account.enabled());
        assertEquals(1, account.authenticationGeneration());
        assertEquals(1, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertTrue(account.isLockedAt(now));
        service.enable(target);
        assertTrue(account.enabled());
        assertEquals(2, account.authenticationGeneration());
        assertEquals(0, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertNull(ReflectionTestUtils.getField(account, "lockedUntilUtc"));
        assertEquals(target, account.publicId());
        assertEquals("synthetic.target", account.canonicalLogin());
        assertTrue(credential.equals(account.passwordHash()));
        assertEquals(timestamp, ReflectionTestUtils.getField(account, "credentialUpdatedAtUtc"));
        assertSame(roles, ReflectionTestUtils.getField(account, "roles"));
        var order = inOrder(actor, transactions, accounts, entities, audit, sessions);
        for (String action : List.of("IDENTITY_ACCOUNT_DISABLED", "IDENTITY_ACCOUNT_ENABLED")) {
            order.verify(actor).requireAuthority("identity:admin");
            order.verify(actor).requireUserId();
            order.verify(transactions).getTransaction(any());
            order.verify(accounts).findByPublicId(target);
            order.verify(entities).refresh(account);
            order.verify(audit).record(new AuditRequest(administrator.toString(), action,
                    "IDENTITY_ACCOUNT", target.toString(), null, "administrative-account-lifecycle"));
            order.verify(accounts).flush();
            order.verify(sessions).revoke(target);
            order.verify(transactions).commit(any());
        }
        verifyNoMoreInteractions(actor, transactions, accounts, entities, audit, sessions);
        assertEquals(1, AccountLifecycleService.class.getMethod("enable", UUID.class).getParameterCount());
        assertEquals(1, AccountLifecycleService.class.getMethod("disable", UUID.class).getParameterCount());
    }

    @Test void realActorRejectsAnonymousUnsupportedAndUnauthenticatedCallersBeforeAnyPersistence() {
        var authorities = List.of(new SimpleGrantedAuthority("identity:admin"));
        for (var token : Arrays.asList(null,
                new AnonymousAuthenticationToken("synthetic", "anonymousUser", authorities),
                UsernamePasswordAuthenticationToken.authenticated("unsupported", null, authorities),
                UsernamePasswordAuthenticationToken.unauthenticated(new AccountPrincipal(administrator, "synthetic.admin"), null))) {
            SecurityContextHolder.getContext().setAuthentication(token);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service(new SecurityCurrentActor()).disable(target));
        }
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(administrator, "synthetic.admin"), null, List.of()));
        assertThrows(AccessDeniedException.class, () -> service(new SecurityCurrentActor()).enable(target));
        verifyNoInteractions(accounts, entities, transactions, audit, sessions);
    }

    @Test void realActorDerivesAdministratorAndAcceptsOnlyStableTarget() {
        ready();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(administrator, "synthetic.admin"), null, List.of(new SimpleGrantedAuthority("identity:admin"))));
        service(new SecurityCurrentActor()).disable(target);
        verifyNoInteractions(actor);
        verify(accounts).findByPublicId(target);
        verify(audit).record(new AuditRequest(administrator.toString(), "IDENTITY_ACCOUNT_DISABLED",
                "IDENTITY_ACCOUNT", target.toString(), null, "administrative-account-lifecycle"));
    }

    @Test void selfDisableInvalidTargetAndAmbientTransactionAreRejectedBeforeMutation() {
        ready();
        bounded(AccountLifecycleException.Reason.SELF_DISABLE_REJECTED, () -> service.disable(administrator));
        bounded(AccountLifecycleException.Reason.INVALID_TARGET, () -> service.enable(null));
        TransactionSynchronizationManager.setActualTransactionActive(true);
        bounded(AccountLifecycleException.Reason.PERSISTENCE_FAILED, () -> service.disable(target));
        verifyNoInteractions(accounts, entities, transactions, audit, sessions);
        assertEquals(0, account.authenticationGeneration());
    }

    @Test void missingAndNoOpTransitionsAreNotRetriedAuditedOrRevoked() {
        ready();
        when(accounts.findByPublicId(target)).thenReturn(Optional.empty());
        bounded(AccountLifecycleException.Reason.ACCOUNT_UNAVAILABLE, () -> service.disable(target));
        when(accounts.findByPublicId(target)).thenReturn(Optional.of(account));
        bounded(AccountLifecycleException.Reason.ALREADY_ENABLED, () -> service.enable(target));
        ReflectionTestUtils.setField(account, "enabled", false);
        bounded(AccountLifecycleException.Reason.ALREADY_DISABLED, () -> service.disable(target));
        assertEquals(0, account.authenticationGeneration());
        verify(transactions, times(3)).rollback(any());
        verifyNoInteractions(audit, sessions);
    }

    @Test void managedEntityIsRefreshedBeforeNoOpCheck() {
        ready();
        doAnswer(call -> { ReflectionTestUtils.setField(account, "enabled", false); return null; }).when(entities).refresh(account);
        service.enable(target);
        assertTrue(account.enabled());
        assertEquals(1, account.authenticationGeneration());
    }

    @Test void failuresAreBoundedAndExpiryOccursOnlyAfterSuccessfulAuditAndFlush() {
        for (int scenario = 0; scenario < 6; scenario++) {
            reset(accounts, entities, transactions, audit, sessions); ready();
            ReflectionTestUtils.setField(account, "enabled", true);
            var internal = new IllegalStateException("Synthetic internal failure");
            switch (scenario) {
                case 0 -> when(accounts.findByPublicId(target)).thenThrow(internal);
                case 1 -> doThrow(internal).when(entities).refresh(account);
                case 2 -> when(audit.record(any())).thenThrow(internal);
                case 3 -> doThrow(internal).when(accounts).flush();
                case 4 -> doThrow(internal).when(sessions).revoke(target);
                case 5 -> doThrow(internal).when(transactions).commit(any());
                default -> fail();
            }
            bounded(scenario == 4 ? AccountLifecycleException.Reason.SESSION_REVOCATION_FAILED
                    : AccountLifecycleException.Reason.PERSISTENCE_FAILED, () -> service.disable(target));
            if (scenario < 4) { verifyNoInteractions(sessions); }
            if (scenario < 5) { verify(transactions).rollback(any()); }
            if (scenario == 5) { verify(sessions).revoke(target); }
        }
    }

    @Test void generationNeverWrapsAndOrdinaryBookkeepingDoesNotAdvanceIt() {
        account.recordFailure(now, 1, Duration.ofMinutes(1));
        account.clearExpiredLock(now.plusSeconds(61));
        account.recordSuccess(now);
        assertEquals(0, account.authenticationGeneration());
        ReflectionTestUtils.setField(account, "authenticationGeneration", Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> account.changeEnabled(false, now));
        assertTrue(account.enabled());
        assertEquals(Long.MAX_VALUE, account.authenticationGeneration());
    }

    private static void bounded(AccountLifecycleException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(AccountLifecycleException.class, action);
        assertEquals(reason, failure.reason());
        assertEquals("Account lifecycle failed: " + reason, failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test void recentProofDenialPrecedesCredentialWorkAndMutation() {
        doThrow(new RecentAuthenticationException(RecentAuthenticationException.Reason.PROOF_REQUIRED))
                .when(recent).requireRecentAuthentication();
        assertThrows(RecentAuthenticationException.class, () -> service.disable(UUID.randomUUID()));
        var order = inOrder(actor, recent);
        order.verify(actor).requireAuthority("identity:admin");
        order.verify(recent).requireRecentAuthentication();
        verifyNoInteractions(transactions, accounts, audit, sessions);

    }
}

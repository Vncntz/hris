package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final RecentAuthenticationGuard recent = mock(RecentAuthenticationGuard.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final AuthenticatedSessionRevoker sessions = mock(AuthenticatedSessionRevoker.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID administrator = UUID.randomUUID(), target = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-03T08:00:00.123456789Z");
    private final String replacement = UUID.randomUUID().toString();
    private final AccountEntity account = new AccountEntity(target, "synthetic.target", UUID.randomUUID().toString(), Instant.EPOCH);
    private final PasswordResetService service = new PasswordResetService(actor, recent, accounts, entities,
            encoder, audit, sessions, Clock.fixed(now, ZoneOffset.UTC), transactions);

    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }
    private void ready() {
        when(actor.requireUserId()).thenReturn(administrator);
        when(accounts.findAuthenticationGeneration(target)).thenReturn(Optional.of(0L));
        when(accounts.findByPublicId(target)).thenReturn(Optional.of(account));
        when(encoder.encode(any())).thenReturn(replacement);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
    private char[][] buffers() {
        char[] password = UUID.randomUUID().toString().toCharArray();
        return new char[][]{password, password.clone()};
    }
    private void invoke(UUID id, char[][] buffers) {
        try { service.reset(id, buffers[0], buffers[1]); }
        finally {
            for (char[] buffer : buffers) {
                if (buffer != null) { for (char value : buffer) { assertEquals(0, value); } }
            }
        }
    }
    private void rejected(PasswordResetException.Reason reason, UUID id, char[][] buffers) {
        var failure = assertThrows(PasswordResetException.class, () -> invoke(id, buffers));
        assertEquals(reason, failure.reason());
        assertEquals("Password reset failed: " + reason, failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test void authorityDenialPrecedesProofValidationAndClearsSecrets() {
        doThrow(new AccessDeniedException("Denied")).when(actor).requireAuthority("identity:admin");
        assertThrows(AccessDeniedException.class, () -> invoke(null, buffers()));
        verifyNoInteractions(recent, accounts, encoder, transactions, audit, sessions);
    }
    @Test void proofDenialPrecedesTargetValidationAndClearsSecrets() {
        doThrow(new RecentAuthenticationException(RecentAuthenticationException.Reason.PROOF_REQUIRED))
                .when(recent).requireRecentAuthentication();
        assertThrows(RecentAuthenticationException.class, () -> invoke(null, buffers()));
        verify(actor).requireAuthority("identity:admin");
        verify(actor, never()).requireUserId();
        verifyNoInteractions(accounts, encoder, transactions, audit, sessions);
    }
    @Test void invalidAndSelfTargetsNeverHash() {
        ready();
        rejected(PasswordResetException.Reason.INVALID_TARGET, null, buffers());
        rejected(PasswordResetException.Reason.SELF_RESET_REJECTED, administrator, buffers());
        verifyNoInteractions(accounts, encoder, transactions, audit, sessions);
    }
    @Test void credentialValidationPreservesUnicodePolicyAndBounds() {
        ready();
        rejected(PasswordResetException.Reason.INVALID_CREDENTIAL, target, new char[][]{null, buffers()[1]});
        var shortValue = new char[11];
        rejected(PasswordResetException.Reason.INVALID_CREDENTIAL, target, new char[][]{shortValue, shortValue.clone()});
        var longValue = new char[129];
        rejected(PasswordResetException.Reason.INVALID_CREDENTIAL, target, new char[][]{longValue, longValue.clone()});
        var malformed = buffers(); malformed[0][0] = '\ud800'; malformed[1] = malformed[0].clone();
        rejected(PasswordResetException.Reason.INVALID_CREDENTIAL, target, malformed);
        var mismatch = buffers(); mismatch[1][0] ^= 1;
        rejected(PasswordResetException.Reason.INVALID_CREDENTIAL, target, mismatch);
        verifyNoInteractions(accounts, encoder, transactions, audit, sessions);
        // Twelve supplementary code points are valid; exact code units are preserved for encoding.
        char[] unicode = new char[24];
        for (int i = 0; i < unicode.length; i += 2) { unicode[i] = '\ud83d'; unicode[i + 1] = '\ude00'; }
        doAnswer(call -> {
            assertEquals(12, Character.codePointCount(call.getArgument(0).toString(), 0, 24));
            return replacement;
        }).when(encoder).encode(any());
        invoke(target, new char[][]{unicode, unicode.clone()});
    }
    @Test void ambientTransactionRefusedBeforeReadAndHash() {
        ready(); TransactionSynchronizationManager.setActualTransactionActive(true);
        rejected(PasswordResetException.Reason.PERSISTENCE_FAILED, target, buffers());
        verifyNoInteractions(accounts, encoder, transactions, audit, sessions);
    }
    @Test void disabledTargetRemainsDisabledWithOnlyCredentialSecurityStateChangedInOrder() {
        ready();
        ReflectionTestUtils.setField(account, "enabled", false);
        account.recordFailure(Instant.EPOCH, 1, java.time.Duration.ofDays(1));
        var role = new RoleEntity(UUID.randomUUID(), "synthetic.role", true);
        account.assignBootstrapRole(role);
        invoke(target, buffers());
        assertFalse(account.enabled()); assertEquals(target, account.publicId());
        assertEquals("synthetic.target", account.canonicalLogin());
        assertEquals(java.util.Set.of(role), account.assignedRoles());
        assertTrue(replacement.equals(account.passwordHash()));
        assertEquals(1, account.authenticationGeneration());
        assertEquals(0, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertNull(ReflectionTestUtils.getField(account, "lockedUntilUtc"));
        var expected = java.time.LocalDateTime.ofInstant(now.truncatedTo(java.time.temporal.ChronoUnit.MICROS), ZoneOffset.UTC);
        assertEquals(expected, ReflectionTestUtils.getField(account, "credentialUpdatedAtUtc"));
        assertEquals(expected, ReflectionTestUtils.getField(account, "securityUpdatedAtUtc"));
        var order = inOrder(actor, recent, accounts, entities, encoder, transactions, audit, sessions);
        order.verify(actor).requireAuthority("identity:admin");
        order.verify(recent).requireRecentAuthentication();
        order.verify(actor).requireUserId();
        order.verify(accounts).findAuthenticationGeneration(target);
        order.verify(encoder).encode(any());
        var definition = org.mockito.ArgumentCaptor.forClass(TransactionDefinition.class);
        order.verify(transactions).getTransaction(definition.capture());
        assertEquals(15, definition.getValue().getTimeout());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getValue().getPropagationBehavior());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getValue().getIsolationLevel());
        order.verify(accounts).findByPublicId(target); order.verify(entities).refresh(account);
        order.verify(audit).record(new AuditRequest(administrator.toString(), "IDENTITY_PASSWORD_RESET",
                "IDENTITY_ACCOUNT", target.toString(), null, "administrative-password-reset"));
        order.verify(accounts).flush(); order.verify(sessions).revoke(target);
        order.verify(transactions).commit(any());
    }
    @Test void missingTargetAndSnapshotFailureAreBounded() {
        ready(); when(accounts.findAuthenticationGeneration(target)).thenReturn(Optional.empty());
        rejected(PasswordResetException.Reason.ACCOUNT_UNAVAILABLE, target, buffers());
        when(accounts.findAuthenticationGeneration(target)).thenThrow(new IllegalStateException());
        rejected(PasswordResetException.Reason.PERSISTENCE_FAILED, target, buffers());
        verifyNoInteractions(encoder, transactions, audit, sessions);
    }
    @Test void encoderFailuresNeverOpenMutationTransaction() {
        ready(); when(encoder.encode(any())).thenThrow(new IllegalStateException());
        rejected(PasswordResetException.Reason.ENCODING_FAILED, target, buffers());
        doReturn(null, " ").when(encoder).encode(any());
        rejected(PasswordResetException.Reason.ENCODING_FAILED, target, buffers());
        rejected(PasswordResetException.Reason.ENCODING_FAILED, target, buffers());
        verifyNoInteractions(transactions, audit, sessions);
    }
    @Test void refreshDetectsStaleGenerationBeforeMutation() {
        ready(); doAnswer(call -> { account.changeEnabled(false, now); return null; }).when(entities).refresh(account);
        rejected(PasswordResetException.Reason.STALE_ACCOUNT, target, buffers());
        verifyNoInteractions(audit, sessions); verify(transactions).rollback(any());
    }
    @Test void auditFlushRevocationAndCommitFailuresAreSanitized() {
        ready(); doThrow(new IllegalStateException()).when(audit).record(any());
        rejected(PasswordResetException.Reason.PERSISTENCE_FAILED, target, buffers());
        verify(sessions, never()).revoke(any(UUID.class)); verify(transactions).rollback(any());
        reset(audit); when(accounts.findAuthenticationGeneration(target)).thenReturn(Optional.of(1L));
        doThrow(new IllegalStateException()).when(accounts).flush();
        rejected(PasswordResetException.Reason.PERSISTENCE_FAILED, target, buffers());
        verify(sessions, never()).revoke(any(UUID.class));
        doNothing().when(accounts).flush(); when(accounts.findAuthenticationGeneration(target)).thenReturn(Optional.of(2L));
        doThrow(new IllegalStateException()).when(sessions).revoke(target);
        rejected(PasswordResetException.Reason.SESSION_REVOCATION_FAILED, target, buffers());
        doNothing().when(sessions).revoke(target); when(accounts.findAuthenticationGeneration(target)).thenReturn(Optional.of(3L));
        doThrow(new IllegalStateException()).when(transactions).commit(any());
        rejected(PasswordResetException.Reason.PERSISTENCE_FAILED, target, buffers());
    }
}

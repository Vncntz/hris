package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.time.Clock;
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
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordChangeServiceTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final AuthenticatedSessionRevoker sessions = mock(AuthenticatedSessionRevoker.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-02T04:05:06.123456789Z");
    private final String oldEncoding = UUID.randomUUID().toString();
    private final String replacement = UUID.randomUUID().toString();
    private final AccountEntity account = new AccountEntity(id, "synthetic.self", oldEncoding, Instant.EPOCH);
    private final PasswordChangeService service = service(actor);

    private PasswordChangeService service(CurrentActor current) {
        return new PasswordChangeService(current, accounts, entities, encoder, audit, sessions,
                Clock.fixed(now, ZoneOffset.UTC), transactions);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
    }

    private void ready() {
        when(actor.requireUserId()).thenReturn(id);
        when(accounts.findEnabledCredential(id)).thenReturn(Optional.of(oldEncoding));
        when(accounts.findByPublicId(id)).thenReturn(Optional.of(account));
        when(encoder.matches(any(), eq(oldEncoding))).thenReturn(true);
        when(encoder.encode(any())).thenReturn(replacement);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void selfOnlyMutationPreservesIdentityRolesAndResetsSecurityWithOrderedFlushExpiryCommit() throws Exception {
        ready();
        var role = new RoleEntity(UUID.randomUUID(), "synthetic.role", true);
        account.assignBootstrapRole(role);
        account.recordFailure(Instant.EPOCH, 1, java.time.Duration.ofDays(1));
        Object roles = ReflectionTestUtils.getField(account, "roles");
        char[][] buffers = buffers();
        service.change(buffers[0], buffers[1], buffers[2]);
        cleared(buffers);
        assertEquals(id, account.publicId());
        assertEquals("synthetic.self", account.canonicalLogin());
        assertTrue(account.enabled());
        assertTrue(replacement.equals(account.passwordHash()));
        assertSame(roles, ReflectionTestUtils.getField(account, "roles"));
        assertEquals(java.util.Set.of(role), roles);
        assertEquals(0, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertNull(ReflectionTestUtils.getField(account, "lockedUntilUtc"));
        var expected = java.time.LocalDateTime.ofInstant(now.truncatedTo(java.time.temporal.ChronoUnit.MICROS), ZoneOffset.UTC);
        assertEquals(expected, ReflectionTestUtils.getField(account, "credentialUpdatedAtUtc"));
        assertEquals(expected, ReflectionTestUtils.getField(account, "securityUpdatedAtUtc"));
        var order = inOrder(actor, accounts, entities, encoder, transactions, audit, sessions);
        order.verify(actor).requireUserId();
        order.verify(accounts).findEnabledCredential(id);
        order.verify(encoder).matches(any(CharBuffer.class), eq(oldEncoding));
        order.verify(encoder).encode(any(CharBuffer.class));
        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        order.verify(transactions).getTransaction(definition.capture());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getValue().getPropagationBehavior());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getValue().getIsolationLevel());
        assertEquals(15, definition.getValue().getTimeout());
        order.verify(accounts).findByPublicId(id);
        order.verify(entities).refresh(account);
        order.verify(audit).record(new AuditRequest(id.toString(), "IDENTITY_PASSWORD_CHANGED",
                "IDENTITY_ACCOUNT", id.toString(), null, "self-service-password-change"));
        order.verify(accounts).flush();
        order.verify(sessions).revoke(id);
        order.verify(transactions).commit(any());
        verifyNoMoreInteractions(actor, accounts, entities, encoder, audit, sessions, transactions);
        assertEquals(3, PasswordChangeService.class.getMethod("change", char[].class, char[].class, char[].class).getParameterCount());
    }

    @Test
    void realActorRequiresSupportedAuthenticatedPrincipalBeforeAnyCredentialWork() {
        var real = service(new SecurityCurrentActor());
        var authorities = List.of(new SimpleGrantedAuthority("identity:admin"));
        var principal = new AccountPrincipal(id, "synthetic.self");
        for (var token : Arrays.asList(null,
                new AnonymousAuthenticationToken("synthetic", "anonymousUser", authorities),
                UsernamePasswordAuthenticationToken.authenticated("unsupported", null, authorities),
                UsernamePasswordAuthenticationToken.unauthenticated(principal, null))) {
            SecurityContextHolder.getContext().setAuthentication(token);
            char[][] buffers = buffers();
            assertThrows(AuthenticationCredentialsNotFoundException.class,
                    () -> real.change(buffers[0], buffers[1], buffers[2]));
            cleared(buffers);
        }
        verifyNoInteractions(accounts, entities, encoder, transactions, audit, sessions);
    }

    @Test
    void zeroAuthorityAccountUsesRealCurrentActorPublicIdWithoutAuthorityShortcut() {
        ready();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(id, "synthetic.self"), null, List.of()));
        char[][] buffers = buffers();
        service(new SecurityCurrentActor()).change(buffers[0], buffers[1], buffers[2]);
        cleared(buffers);
        verify(accounts).findByPublicId(id);
        verifyNoInteractions(actor);
    }

    @Test
    void wrongOrMissingCurrentCredentialClearsAllBuffersWithoutEncodingMutationOrAudit() {
        ready();
        when(encoder.matches(any(), any())).thenReturn(false);
        for (int scenario = 0; scenario < 2; scenario++) {
            char[][] buffers = buffers();
            if (scenario == 1) { buffers[0] = null; }
            bounded(PasswordChangeException.Reason.CURRENT_CREDENTIAL_REJECTED, () ->
                    service.change(buffers[0], buffers[1], buffers[2]));
            cleared(buffers);
        }
        verify(encoder, never()).encode(any());
        verifyNoInteractions(transactions, audit, sessions);
    }

    @Test
    void sharedCredentialValidationRejectsBoundsConfirmationMalformedUnicodeAndCancellation() {
        for (int scenario = 0; scenario < 9; scenario++) {
            char[][] buffers = buffers();
            if (scenario == 0) { buffers[1] = new char[11]; }
            if (scenario == 1) { buffers[1] = new char[129]; }
            if (scenario == 2) { buffers[1][0] = '\uD800'; }
            if (scenario == 3) { buffers[1][0] = '\uDC00'; }
            if (scenario == 4) { buffers[1][buffers[1].length - 1] = '\uD800'; }
            if (scenario == 5) { buffers[2][0] ^= 1; }
            if (scenario == 6) { buffers[1] = null; }
            if (scenario == 7) { buffers[2] = null; }
            if (scenario == 8) { buffers[1] = new char[0]; }
            assertThrows(IllegalArgumentException.class, () -> service.change(buffers[0], buffers[1], buffers[2]));
            cleared(buffers);
        }
        verifyNoInteractions(accounts, entities, encoder, transactions, audit, sessions);
    }

    @Test
    void exactCodeUnitsWhitespaceAndUnicodeEndpointsReachEncoderOutsideTransaction() {
        ready();
        for (int length : new int[]{12, 128}) {
            for (boolean supplementary : new boolean[]{false, true}) {
                char[] password = new char[length * (supplementary ? 2 : 1)];
                char[] random = UUID.randomUUID().toString().toCharArray();
                char[] pair = Character.toChars(0x10000 + new java.security.SecureRandom().nextInt(0xFFFFF));
                for (int i = 0; i < password.length; i++) {
                    password[i] = supplementary ? pair[i % 2] : random[i % random.length];
                }
                if (!supplementary) { password[0] = ' '; password[password.length - 1] = ' '; }
                InitialCredentials.clear(random);
                char[] expected = password.clone(), current = UUID.randomUUID().toString().toCharArray();
                doAnswer(call -> {
                    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                    assertTrue(equalCodeUnits(call.getArgument(0), current));
                    return true;
                }).when(encoder).matches(any(), eq(oldEncoding));
                doAnswer(call -> {
                    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                    assertTrue(equalCodeUnits(call.getArgument(0), expected));
                    return replacement;
                }).when(encoder).encode(any());
                account.changePassword(oldEncoding, Instant.EPOCH);
                char[] confirmation = password.clone();
                service.change(current, password, confirmation);
                cleared(current, password, confirmation);
                InitialCredentials.clear(expected);
            }
        }
    }

    @Test
    void ambientTransactionRefusedBeforeReadingOrVerifyingCredentials() {
        ready();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        char[][] buffers = buffers();
        bounded(PasswordChangeException.Reason.PERSISTENCE_FAILED,
                () -> service.change(buffers[0], buffers[1], buffers[2]));
        cleared(buffers);
        verifyNoInteractions(accounts, entities, encoder, transactions, audit, sessions);
    }

    @Test
    void staleCredentialIsNeverRetriedOrOverwritten() {
        ready();
        account.changePassword(UUID.randomUUID().toString(), Instant.EPOCH);
        String before = account.passwordHash();
        char[][] buffers = buffers();
        bounded(PasswordChangeException.Reason.STALE_CREDENTIAL,
                () -> service.change(buffers[0], buffers[1], buffers[2]));
        cleared(buffers);
        assertTrue(before.equals(account.passwordHash()));
        verify(encoder, times(1)).matches(any(), any());
        verify(transactions).rollback(any());
        verifyNoInteractions(audit, sessions);
    }

    @Test
    void accountMissingOrDisabledAtReadAndLockFailsClosed() {
        for (int scenario = 0; scenario < 3; scenario++) {
            reset(accounts, encoder, transactions, audit, sessions); ready();
            if (scenario == 0) { when(accounts.findEnabledCredential(id)).thenReturn(Optional.empty()); }
            if (scenario == 1) { when(accounts.findByPublicId(id)).thenReturn(Optional.empty()); }
            if (scenario == 2) { ReflectionTestUtils.setField(account, "enabled", false); }
            char[][] buffers = buffers();
            bounded(PasswordChangeException.Reason.ACCOUNT_UNAVAILABLE,
                    () -> service.change(buffers[0], buffers[1], buffers[2]));
            cleared(buffers);
            assertTrue(oldEncoding.equals(account.passwordHash()));
            verifyNoInteractions(audit, sessions);
        }
    }

    @Test
    void infrastructureFailuresAreBoundedClearBuffersAndRespectExpiryOrdering() {
        for (int scenario = 0; scenario < 9; scenario++) {
            reset(accounts, entities, encoder, transactions, audit, sessions); ready();
            account.changePassword(oldEncoding, Instant.EPOCH);
            var internal = new IllegalStateException("Synthetic internal failure " + UUID.randomUUID());
            switch (scenario) {
                case 0 -> when(accounts.findEnabledCredential(id)).thenThrow(internal);
                case 1 -> when(encoder.matches(any(), any())).thenThrow(internal);
                case 2 -> when(encoder.encode(any())).thenThrow(internal);
                case 3 -> when(accounts.findByPublicId(id)).thenThrow(internal);
                case 4 -> doThrow(internal).when(entities).refresh(account);
                case 5 -> when(audit.record(any())).thenThrow(internal);
                case 6 -> doThrow(internal).when(accounts).flush();
                case 7 -> doThrow(new SessionRevocationException())
                        .when(sessions).revoke(id);
                case 8 -> doThrow(internal).when(transactions).commit(any());
                default -> fail();
            }
            char[][] buffers = buffers();
            bounded(scenario == 2 ? PasswordChangeException.Reason.ENCODING_FAILED
                    : scenario == 7 ? PasswordChangeException.Reason.SESSION_REVOCATION_FAILED
                    : PasswordChangeException.Reason.PERSISTENCE_FAILED,
                    () -> service.change(buffers[0], buffers[1], buffers[2]));
            cleared(buffers);
            if (scenario < 7) { verifyNoInteractions(sessions); }
            if (scenario >= 3 && scenario <= 7) { verify(transactions).rollback(any()); }
            if (scenario == 8) { verify(sessions).revoke(id); }
        }
    }

    @Test
    void missingEncoderOutputIsRejectedBeforeMutation() {
        ready();
        when(encoder.encode(any())).thenReturn(null);
        char[][] buffers = buffers();
        bounded(PasswordChangeException.Reason.ENCODING_FAILED,
                () -> service.change(buffers[0], buffers[1], buffers[2]));
        cleared(buffers);
        verifyNoInteractions(transactions, audit, sessions);
    }

    private static boolean equalCodeUnits(CharSequence actual, char[] expected) {
        if (actual.length() != expected.length) { return false; }
        for (int i = 0; i < expected.length; i++) { if (actual.charAt(i) != expected[i]) { return false; } }
        return true;
    }

    private static void bounded(PasswordChangeException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(PasswordChangeException.class, action);
        assertEquals(reason, failure.reason());
        assertEquals("Password change failed: " + reason, failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }

    private static char[][] buffers() {
        char[] replacement = UUID.randomUUID().toString().toCharArray();
        return new char[][]{UUID.randomUUID().toString().toCharArray(), replacement, replacement.clone()};
    }

    private static void cleared(char[]... buffers) {
        for (char[] buffer : buffers) {
            if (buffer == null) { continue; }
            boolean empty = true;
            for (char value : buffer) { empty &= value == 0; }
            assertTrue(empty, "Owned credential buffer must be cleared");
        }
    }
}

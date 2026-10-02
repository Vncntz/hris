package io.github.vncntz.hris.identityaccess;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountCreationServiceTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID creator = UUID.randomUUID();
    private final AccountCreationService service = service(actor);

    private AccountCreationService service(CurrentActor current) {
        return new AccountCreationService(current, accounts, encoder, audit, Clock.systemUTC(), transactions);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
    }

    private void ready() {
        when(actor.requireUserId()).thenReturn(creator);
        // An ephemeral encoding stand-in; assertions never print it.
        when(encoder.encode(any())).thenReturn(UUID.randomUUID().toString());
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void authorizedCreationHasOnlyPublicIdAndAuthenticatedAuditActor() {
        ready();
        char[] password = ephemeral(24), confirmation = password.clone();
        var result = service.create(" SYNTHETIC.LOGIN ", password, confirmation);
        assertCleared(password, confirmation);
        var entity = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accounts).saveAndFlush(entity.capture());
        assertEquals(result.publicId(), entity.getValue().publicId());
        assertEquals("synthetic.login", entity.getValue().canonicalLogin());
        assertTrue(entity.getValue().enabled());
        verify(audit).record(new AuditRequest(creator.toString(), "IDENTITY_ACCOUNT_CREATED",
                "IDENTITY_ACCOUNT", result.publicId().toString(), null, "authenticated-account-creation"));
        var order = inOrder(actor, encoder, transactions, accounts, audit);
        order.verify(actor).requireAuthority("identity:admin");
        order.verify(actor).requireUserId();
        order.verify(encoder).encode(any());
        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        order.verify(transactions).getTransaction(definition.capture());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getValue().getIsolationLevel());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getValue().getPropagationBehavior());
        assertEquals(15, definition.getValue().getTimeout());
        order.verify(accounts).saveAndFlush(any());
        order.verify(audit).record(any());
        verify(transactions).commit(any());
        assertEquals("CreatedAccount[publicId=" + result.publicId() + "]", result.toString());
    }

    @Test
    void denialPrecedesValidationEncodingTransactionsAndWritesAndClearsBuffers() {
        doThrow(new AccessDeniedException("Access denied")).when(actor).requireAuthority("identity:admin");
        char[] password = ephemeral(24), confirmation = password.clone();
        assertThrows(AccessDeniedException.class, () -> service.create(null, password, confirmation));
        assertCleared(password, confirmation);
        verify(actor, never()).requireUserId();
        verifyNoInteractions(encoder, transactions, accounts, audit);
    }

    @Test
    void realCurrentActorRejectsAbsentAnonymousUnsupportedUnauthenticatedAndOrdinaryPrincipals() {
        var real = service(new SecurityCurrentActor());
        var authority = List.of(new SimpleGrantedAuthority("identity:admin"));
        var principal = new AccountPrincipal(creator, "synthetic.creator");
        var tokens = Arrays.asList(null,
                new AnonymousAuthenticationToken("synthetic", "anonymousUser", authority),
                UsernamePasswordAuthenticationToken.authenticated("unsupported", null, authority),
                UsernamePasswordAuthenticationToken.unauthenticated(principal, null),
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        for (var token : tokens) {
            SecurityContextHolder.getContext().setAuthentication(token);
            char[] password = ephemeral(24), confirmation = password.clone();
            assertThrows(RuntimeException.class,
                    () -> real.create("synthetic.login", password, confirmation));
            assertCleared(password, confirmation);
        }
        verifyNoInteractions(encoder, transactions, accounts, audit);
    }

    @Test
    void invalidLoginsFailWithoutHashingAndClearBuffers() {
        for (String login : new String[]{null, "", "ab", "bad login", "x".repeat(129), "\u00e9clair"}) {
            char[] password = ephemeral(24), confirmation = password.clone();
            assertThrows(IllegalArgumentException.class, () -> service.create(login, password, confirmation));
            assertCleared(password, confirmation);
        }
        verifyNoInteractions(encoder, transactions, accounts, audit);
    }

    @Test
    void passwordBoundsUnicodeMismatchAndCancellationClearEverySuppliedBuffer() {
        for (int scenario = 0; scenario < 9; scenario++) {
            char[] password = ephemeral(scenario == 0 ? 11 : scenario == 1 ? 129 : 24);
            char[] confirmation = password.clone();
            if (scenario == 2) { password[0] = '\uD800'; }
            if (scenario == 3) { password[0] = '\uDC00'; }
            if (scenario == 4) { password[password.length - 1] = '\uD800'; }
            if (scenario == 5) { confirmation[0] ^= 1; }
            if (scenario == 6) { password = null; }
            if (scenario == 7) { confirmation = null; }
            if (scenario == 8) { password = new char[0]; confirmation = new char[0]; }
            char[] first = password, second = confirmation;
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> service.create("synthetic.login", first, second));
            assertNull(failure.getCause());
            assertCleared(first, second);
        }
        verifyNoInteractions(encoder, transactions, accounts, audit);
    }

    @Test
    void acceptedEndpointsAndSupplementaryCodePointsReachEncoderUnmodified() {
        ready();
        for (int length : new int[]{12, 128}) {
            char[] value = ephemeral(length);
            value[0] = ' '; value[value.length - 1] = ' ';
            createPreserving(value);
            char[] unicode = new char[length * 2];
            int point = 0x10000 + new java.security.SecureRandom().nextInt(0xFFFFF);
            char[] pair = Character.toChars(point);
            for (int i = 0; i < unicode.length; i += 2) { unicode[i] = pair[0]; unicode[i + 1] = pair[1]; }
            createPreserving(unicode);
        }
        char[] over = new char[258];
        for (int i = 0; i < over.length; i += 2) { over[i] = '\uD800'; over[i + 1] = '\uDC00'; }
        char[] confirmation = over.clone();
        assertThrows(IllegalArgumentException.class, () -> service.create("synthetic.login", over, confirmation));
        assertCleared(over, confirmation);
    }

    private void createPreserving(char[] password) {
        char[] expected = password.clone(), confirmation = password.clone();
        doAnswer(call -> {
            CharSequence supplied = call.getArgument(0);
            boolean equal = supplied.length() == expected.length;
            for (int i = 0; equal && i < expected.length; i++) { equal = supplied.charAt(i) == expected[i]; }
            assertTrue(equal, "Encoder must receive exact supplied code units");
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return UUID.randomUUID().toString();
        }).when(encoder).encode(any());
        service.create("synthetic.login", password, confirmation);
        assertCleared(password, confirmation);
        InitialCredentials.clear(expected);
    }

    @Test
    void encoderFailureIsSanitizedAndClearsBuffersBeforeAnyWrite() {
        ready();
        char[] password = ephemeral(24), confirmation = password.clone();
        when(encoder.encode(any())).thenThrow(new IllegalStateException("Synthetic infrastructure failure"));
        var failure = assertThrows(AccountCreationException.class,
                () -> service.create("synthetic.login", password, confirmation));
        assertEquals(AccountCreationException.Reason.ENCODING_FAILED, failure.reason());
        assertNull(failure.getCause());
        assertCleared(password, confirmation);
        verifyNoInteractions(accounts, audit, transactions);
    }

    @Test
    void duplicateOtherIntegrityAuditAndCommitFailuresRollbackAndDoNotExposeCauses() {
        for (int scenario = 0; scenario < 4; scenario++) {
            reset(accounts, audit, transactions); ready();
            char[] password = ephemeral(24), confirmation = password.clone();
            if (scenario < 2) {
                when(accounts.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("internal",
                        new SQLException("internal uq_identity_account_canonical_login", "23000", scenario == 0 ? 1062 : 3819)));
            } else if (scenario == 2) {
                when(audit.record(any())).thenThrow(new IllegalStateException("Synthetic infrastructure failure"));
            } else {
                doThrow(new IllegalStateException("Synthetic infrastructure failure")).when(transactions).commit(any());
            }
            var failure = assertThrows(AccountCreationException.class,
                    () -> service.create("synthetic.login", password, confirmation));
            assertEquals(scenario == 0 ? AccountCreationException.Reason.DUPLICATE_LOGIN
                    : AccountCreationException.Reason.PERSISTENCE_FAILED, failure.reason());
            assertNull(failure.getCause());
            assertEquals("Account creation failed: " + failure.reason(), failure.getMessage());
            assertCleared(password, confirmation);
            if (scenario < 3) { verify(transactions).rollback(any()); }
            if (scenario < 2) { verifyNoInteractions(audit); }
        }
    }

    @Test
    void ambientTransactionIsRefusedBeforeEncodingAndClearsSecrets() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        char[] password = ephemeral(24), confirmation = password.clone();
        assertThrows(AccountCreationException.class, () -> service.create("synthetic.login", password, confirmation));
        assertCleared(password, confirmation);
        verifyNoInteractions(encoder, accounts, audit, transactions);
    }

    private static char[] ephemeral(int length) {
        char[] value = new char[length], random = UUID.randomUUID().toString().toCharArray();
        for (int i = 0; i < length; i++) { value[i] = random[i % random.length]; }
        InitialCredentials.clear(random);
        return value;
    }

    private static void assertCleared(char[]... buffers) {
        for (char[] buffer : buffers) {
            if (buffer == null) { continue; }
            boolean cleared = true;
            for (char item : buffer) { cleared &= item == 0; }
            assertTrue(cleared, "Owned secret buffer must be cleared");
        }
    }
}

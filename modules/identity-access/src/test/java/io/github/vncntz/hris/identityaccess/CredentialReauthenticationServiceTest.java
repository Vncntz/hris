package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static io.github.vncntz.hris.identityaccess.RecentAuthenticationException.Reason.*;

class CredentialReauthenticationServiceTest {
    final CurrentActor actor = mock(CurrentActor.class);
    final RecentAuthenticationSession session = mock(RecentAuthenticationSession.class);
    final AccountRepository accounts = mock(AccountRepository.class);
    final EntityManager entities = mock(EntityManager.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final UUID id = UUID.randomUUID();
    final String encoding = UUID.randomUUID().toString();
    final Instant now = Instant.parse("2026-10-03T04:00:00Z");
    final SecurityPolicy policy = new SecurityPolicy(3, java.time.Duration.ofMinutes(7));
    final AccountRepository.AuthenticationState snapshot = mock(AccountRepository.AuthenticationState.class);
    final AccountEntity account = new AccountEntity(id, "synthetic.self", encoding, Instant.EPOCH);
    final CredentialReauthenticationService service = new CredentialReauthenticationService(actor, session,
            accounts, entities, encoder, Clock.fixed(now, ZoneOffset.UTC), policy, transactions);
    RecentAuthenticationSession.Proof published;

    void ready() {
        when(actor.requireUserId()).thenReturn(id);
        when(accounts.findEnabledAuthenticationState(id)).thenReturn(Optional.of(snapshot));
        when(snapshot.getPasswordHash()).thenReturn(encoding);
        when(accounts.findByPublicId(id)).thenReturn(Optional.of(account));
        when(encoder.matches(any(), eq(encoding))).thenReturn(true);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doAnswer(call -> {
            published = null;
            Supplier<RecentAuthenticationSession.Proof> verify = call.getArgument(0);
            published = verify.get();
            return null;
        }).when(session).attempt(any());
    }
    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }

    @Test void expiredLockRecoveryCommitsBeforePublicationAndUsesBoundedTransaction() {
        ready();
        account.recordFailure(Instant.EPOCH, 1, java.time.Duration.ofDays(1));
        Object credentialTime = ReflectionTestUtils.getField(account, "credentialUpdatedAtUtc");
        doAnswer(call -> { assertNull(published); return null; }).when(transactions).commit(any());
        char[] password = secret(); service.reauthenticate(password); cleared(password);
        assertEquals(new RecentAuthenticationSession.Proof(id, now), published);
        assertEquals(0, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertNull(ReflectionTestUtils.getField(account, "lockedUntilUtc"));
        assertEquals(java.time.LocalDateTime.ofInstant(now, ZoneOffset.UTC), ReflectionTestUtils.getField(account, "securityUpdatedAtUtc"));
        assertEquals(credentialTime, ReflectionTestUtils.getField(account, "credentialUpdatedAtUtc"));
        assertTrue(encoding.equals(account.passwordHash()));
        assertEquals(0, account.authenticationGeneration());
        var order = inOrder(actor, accounts, encoder, transactions, entities);
        order.verify(actor).requireUserId();
        order.verify(accounts).findEnabledAuthenticationState(id);
        order.verify(encoder).matches(any(), eq(encoding));
        var definition = org.mockito.ArgumentCaptor.forClass(TransactionDefinition.class);
        order.verify(transactions).getTransaction(definition.capture());
        assertEquals(15, definition.getValue().getTimeout());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getValue().getIsolationLevel());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getValue().getPropagationBehavior());
        order.verify(accounts).findByPublicId(id); order.verify(entities).refresh(account);
        order.verify(transactions).commit(any());
        verify(accounts, never()).save(any()); verify(accounts, never()).flush();
    }
    @Test void wrongCredentialCommitsExactlyOneFailureBeforeRejectionAndClearsPreviousProof() {
        ready(); published = new RecentAuthenticationSession.Proof(id, now);
        when(encoder.matches(any(), eq(encoding))).thenReturn(false);
        doAnswer(call -> {
            assertNull(published);
            assertEquals(1, ReflectionTestUtils.getField(account, "failedAttempts"));
            return null;
        }).when(transactions).commit(any());
        reject(CREDENTIAL_REJECTED);
        verify(transactions).commit(any()); verify(transactions, never()).rollback(any());
        assertEquals(1, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertNull(ReflectionTestUtils.getField(account, "lockedUntilUtc"));
    }
    @Test void missingOrDisabledSnapshotFailsClosed() {
        ready(); when(accounts.findEnabledAuthenticationState(id)).thenReturn(Optional.empty());
        reject(ACCOUNT_UNAVAILABLE); verifyNoInteractions(encoder, transactions, entities);
    }
    @Test void missingOrDisabledLockedAccountFailsClosed() {
        ready(); when(accounts.findByPublicId(id)).thenReturn(Optional.empty()); reject(ACCOUNT_UNAVAILABLE);
        when(accounts.findByPublicId(id)).thenReturn(Optional.of(account));
        account.changeEnabled(false, now); reject(ACCOUNT_UNAVAILABLE);
    }
    @Test void refreshRejectsConcurrentCredentialReplacement() {
        ready(); doAnswer(call -> { account.changePassword(UUID.randomUUID().toString(), now); return null; })
                .when(entities).refresh(account);
        reject(STALE_CREDENTIAL); verify(transactions).rollback(any());
    }
    @Test void callerTransactionIsRefusedBeforeCredentialWork() {
        ready(); TransactionSynchronizationManager.setActualTransactionActive(true);
        reject(PERSISTENCE_FAILED); verifyNoInteractions(accounts, encoder, transactions, entities);
    }
    @Test void persistenceReadEncoderRefreshAndCommitFailuresAreBoundedAndNeverPublish() {
        for (int stage = 0; stage < 4; stage++) {
            reset(accounts, encoder, entities, transactions); ready();
            RuntimeException privateFailure = new IllegalStateException(UUID.randomUUID().toString());
            if (stage == 0) { when(accounts.findEnabledAuthenticationState(id)).thenThrow(privateFailure); }
            if (stage == 1) { when(encoder.matches(any(), eq(encoding))).thenThrow(privateFailure); }
            if (stage == 2) { doThrow(privateFailure).when(entities).refresh(account); }
            if (stage == 3) { doThrow(privateFailure).when(transactions).commit(any()); }
            reject(PERSISTENCE_FAILED);
        }
    }
    @Test void observedActiveLockRejectsCorrectCredentialBeforeAdaptiveWork() {
        ready();
        when(snapshot.getLockedUntilUtc()).thenReturn(java.time.LocalDateTime.ofInstant(now.plusSeconds(1), ZoneOffset.UTC));
        reject(CREDENTIAL_REJECTED); verifyNoInteractions(encoder, transactions, entities);
    }
    @Test void lockCreatedAfterSnapshotRejectsMatchedCredentialWithoutExtendingLock() {
        ready();
        doAnswer(call -> { account.recordFailure(now, 1, policy.lockDuration()); return null; })
                .when(entities).refresh(account);
        reject(CREDENTIAL_REJECTED); verify(transactions).rollback(any());
        assertTrue(account.isLockedAt(now));
        assertEquals(1, ReflectionTestUtils.getField(account, "failedAttempts"));
    }
    @Test void exactThresholdUsesSharedConfiguredLimitAndDuration() {
        ready(); when(encoder.matches(any(), eq(encoding))).thenReturn(false);
        for (int attempt = 1; attempt <= policy.maxFailedAttempts(); attempt++) {
            reject(CREDENTIAL_REJECTED);
            assertEquals(attempt, ReflectionTestUtils.getField(account, "failedAttempts"));
            assertEquals(attempt == policy.maxFailedAttempts(), account.isLockedAt(now));
        }
        assertEquals(java.time.LocalDateTime.ofInstant(now.plus(policy.lockDuration()), ZoneOffset.UTC),
                ReflectionTestUtils.getField(account, "lockedUntilUtc"));
        verify(transactions, times(3)).commit(any());
    }
    @Test void staleWrongCredentialCannotIncrementNewCredentialFailures() {
        ready(); when(encoder.matches(any(), eq(encoding))).thenReturn(false);
        doAnswer(call -> { account.changePassword(UUID.randomUUID().toString(), now); return null; })
                .when(entities).refresh(account);
        reject(STALE_CREDENTIAL);
        assertEquals(0, ReflectionTestUtils.getField(account, "failedAttempts"));
        verify(transactions).rollback(any());
    }
    @Test void wrongCredentialCommitFailureIsPersistenceFailureWithoutProofOrPrivateCause() {
        ready(); when(encoder.matches(any(), eq(encoding))).thenReturn(false);
        doThrow(new IllegalStateException(UUID.randomUUID().toString())).when(transactions).commit(any());
        reject(PERSISTENCE_FAILED); verify(transactions).commit(any());
    }
    @Test void absentActorNullBufferAndSessionFailureClearOwnedInput() {
        ready(); when(actor.requireUserId()).thenThrow(new AuthenticationCredentialsNotFoundException("Authentication required"));
        char[] password = secret(); assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> service.reauthenticate(password)); cleared(password); assertNull(published);
        doReturn(id).when(actor).requireUserId();
        var failure = assertThrows(RecentAuthenticationException.class, () -> service.reauthenticate(null));
        assertEquals(CREDENTIAL_REJECTED, failure.reason());
        doThrow(new RecentAuthenticationException(SESSION_UNAVAILABLE)).when(session).attempt(any());
        reject(SESSION_UNAVAILABLE);
    }
    void reject(RecentAuthenticationException.Reason reason) {
        char[] password = secret();
        var failure = assertThrows(RecentAuthenticationException.class, () -> service.reauthenticate(password));
        cleared(password); assertNull(published); assertNull(failure.getCause());
        assertEquals(reason, failure.reason());
        assertEquals("Recent authentication failed: " + reason.name(), failure.getMessage());
    }
    static char[] secret() { return UUID.randomUUID().toString().toCharArray(); }
    static void cleared(char[] buffer) { boolean empty = true; for (char c : buffer) { empty &= c == 0; } assertTrue(empty); }
}

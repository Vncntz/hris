package io.github.vncntz.hris.identityaccess;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountSessionRegistrationServiceTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final Runnable callback = mock(Runnable.class);
    private final AccountEntity account = new AccountEntity(UUID.randomUUID(), "synthetic.login",
            UUID.randomUUID().toString(), Instant.EPOCH);
    private final AccountSessionRegistrationService service =
            new AccountSessionRegistrationService(accounts, entities, transactions);

    private UsernamePasswordAuthenticationToken token() {
        var token = UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(account.publicId(), account.canonicalLogin()), null, List.of());
        token.setDetails(new AuthenticationGeneration(account.authenticationGeneration()));
        return token;
    }

    private void ready() {
        when(accounts.findByPublicId(account.publicId())).thenReturn(Optional.of(account));
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            TransactionDefinition definition = call.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
    }

    @Test
    void registrationRunsAfterLockedRefreshBeforeCommitAndStripsMetadata() {
        ready();
        var token = token();
        service.register(token, callback);
        var order = inOrder(accounts, entities, callback, transactions);
        order.verify(transactions).getTransaction(any());
        order.verify(accounts).findByPublicId(account.publicId());
        order.verify(entities).refresh(account);
        order.verify(callback).run();
        order.verify(transactions).commit(any());
        assertNull(token.getDetails());
        assertNull(token.getCredentials());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void staleOrDisabledStateRejectsBeforeRegistrationAndStripsMetadata() {
        ready();
        var stale = token();
        account.changePassword(UUID.randomUUID().toString(), Instant.EPOCH);
        rejected(stale);
        var disabled = token();
        ReflectionTestUtils.setField(account, "enabled", false);
        rejected(disabled);
        verifyNoInteractions(callback);
    }

    @Test
    void missingAccountOrMissingGenerationFailsClosed() {
        ready();
        var missing = token();
        when(accounts.findByPublicId(account.publicId())).thenReturn(Optional.empty());
        rejected(missing);
        var noMetadata = token();
        noMetadata.setDetails(null);
        rejected(noMetadata);
        verifyNoInteractions(callback);
    }

    @Test
    void refreshAndCommitFailuresExposeOnlyBoundedErrorAndStripMetadata() {
        ready();
        doThrow(new IllegalStateException("Synthetic persistence failure")).when(entities).refresh(account);
        rejected(token());
        verifyNoInteractions(callback);
        reset(entities);
        doThrow(new IllegalStateException("Synthetic commit failure")).when(transactions).commit(any());
        rejected(token());
        verify(callback).run();
    }

    @Test
    void generationStrictlyAdvancesWithSameOrBackwardsClockWithoutChangingIdentity() {
        var original = account.authenticationGeneration();
        account.changePassword(UUID.randomUUID().toString(), Instant.EPOCH);
        var first = account.authenticationGeneration();
        account.changePassword(UUID.randomUUID().toString(), Instant.EPOCH.minusSeconds(1));
        assertTrue(first > original);
        assertTrue(account.authenticationGeneration() > first);
        assertEquals("synthetic.login", account.canonicalLogin());
        assertEquals(java.time.LocalDateTime.ofInstant(Instant.EPOCH.minusSeconds(1), java.time.ZoneOffset.UTC),
                ReflectionTestUtils.getField(account, "credentialUpdatedAtUtc"));
    }

    @Test
    void authenticationBeforeDisableEnableIsStaleEvenThoughAccountIsEnabledAgain() {
        ready();
        var before = token();
        account.changeEnabled(false, Instant.EPOCH);
        account.changeEnabled(true, Instant.EPOCH);
        rejected(before);
        verifyNoInteractions(callback);
        service.register(token(), callback);
        verify(callback).run();
    }

    private void rejected(UsernamePasswordAuthenticationToken token) {
        var failure = assertThrows(SessionAuthenticationException.class, () -> service.register(token, callback));
        assertNull(failure.getCause());
        assertEquals("Authentication could not establish a session", failure.getMessage());
        assertNull(token.getDetails());
    }
}

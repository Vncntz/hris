package io.github.vncntz.hris.platformoperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.BusinessTimeZone;
import io.github.vncntz.hris.sharedkernel.PublicId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgencyConfigurationServiceTest {
    private static final PublicId ACTOR = PublicId.of(UUID.fromString("00000000-0000-0000-0000-000000000007"));
    private static final BusinessTimeZone MANILA = BusinessTimeZone.of("Asia/Manila");
    private final AgencyConfigurationRepository repository = mock(AgencyConfigurationRepository.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final AgencyConfigurationService service = new AgencyConfigurationService(repository, audit,
            Clock.fixed(Instant.parse("2026-09-30T01:02:03Z"), ZoneOffset.ofHours(9)), transactions);

    AgencyConfigurationServiceTest() {
        when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
    }

    @Test
    void namesAreTrimmedBoundedAndRejectControlCharacters() {
        assertEquals("Example Agency", AgencyConfigurationService.validName("  Example Agency  "));
        assertEquals("a".repeat(200), AgencyConfigurationService.validName("a".repeat(200)));
        assertThrows(IllegalArgumentException.class, () -> AgencyConfigurationService.validName(" \t "));
        assertThrows(IllegalArgumentException.class, () -> AgencyConfigurationService.validName("a".repeat(201)));
        assertThrows(IllegalArgumentException.class, () -> AgencyConfigurationService.validName("a\nb"));
        assertThrows(IllegalArgumentException.class, () -> AgencyConfigurationService.validName(null));
    }

    @Test
    void duplicateInitializationFailsBeforeMutationOrAudit() {
        when(repository.existsById((byte) 1)).thenReturn(true);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.initialize("Agency", MANILA, ACTOR));
        assertTrue(failure.getMessage().contains("already initialized"));
        verify(repository, never()).saveAndFlush(any());
        verify(audit, never()).record(any());
    }

    @Test
    void contentionRetriesOnlyAfterFirstTransactionRollsBack() {
        CannotAcquireLockException contention = new CannotAcquireLockException("deadlock");
        when(repository.saveAndFlush(any())).thenThrow(contention).thenAnswer(call -> call.getArgument(0));

        service.initialize("Agency", MANILA, ACTOR);

        InOrder order = inOrder(transactions, repository, audit);
        order.verify(transactions).getTransaction(any());
        order.verify(repository).saveAndFlush(any());
        order.verify(transactions).rollback(any());
        order.verify(transactions).getTransaction(any());
        order.verify(repository).saveAndFlush(any());
        order.verify(audit).record(any());
        order.verify(transactions).commit(any());
        verify(transactions, times(2)).getTransaction(any());
        verify(audit, times(1)).record(any());
    }

    @Test
    void contentionFindsCommittedWinnerInFreshTransactionWithoutSuccessAudit() {
        when(repository.existsById((byte) 1)).thenReturn(false, true);
        when(repository.saveAndFlush(any())).thenThrow(new CannotAcquireLockException("deadlock"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.initialize("Agency", MANILA, ACTOR));

        assertEquals("Agency configuration is already initialized", failure.getMessage());
        InOrder order = inOrder(transactions, repository);
        order.verify(transactions).getTransaction(any());
        order.verify(repository).saveAndFlush(any());
        order.verify(transactions).rollback(any());
        order.verify(transactions).getTransaction(any());
        order.verify(repository).existsById((byte) 1);
        verify(audit, never()).record(any());
    }

    @Test
    void exhaustedContentionWithoutWinnerPropagatesOriginalFailure() {
        CannotAcquireLockException contention = new CannotAcquireLockException("lock timeout");
        when(repository.saveAndFlush(any())).thenThrow(contention);

        assertSame(contention, assertThrows(CannotAcquireLockException.class,
                () -> service.initialize("Agency", MANILA, ACTOR)));

        verify(repository, times(3)).saveAndFlush(any());
        verify(transactions, times(4)).getTransaction(any());
        verify(transactions, times(3)).rollback(any());
        verify(audit, never()).record(any());
    }

    @Test
    void unrelatedIntegrityFailureIsNotRetriedOrMisreportedAsDuplicate() {
        DataIntegrityViolationException invalid = new DataIntegrityViolationException("unrelated constraint");
        when(repository.saveAndFlush(any())).thenThrow(invalid);

        assertSame(invalid, assertThrows(DataIntegrityViolationException.class,
                () -> service.initialize("Agency", MANILA, ACTOR)));

        verify(repository).saveAndFlush(any());
        verify(transactions, times(2)).getTransaction(any());
        verify(audit, never()).record(any());
    }

    @Test
    void integrityFailureReportsDuplicateOnlyAfterFreshReadFindsWinner() {
        when(repository.existsById((byte) 1)).thenReturn(false, true);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("singleton key"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.initialize("Agency", MANILA, ACTOR));

        assertEquals("Agency configuration is already initialized", failure.getMessage());
        verify(transactions, times(2)).getTransaction(any());
        verify(audit, never()).record(any());
    }

    @Test
    void contentionInsideSurroundingTransactionPropagatesWithoutIndependentRetry() {
        CannotAcquireLockException contention = new CannotAcquireLockException("deadlock");
        when(repository.saveAndFlush(any())).thenThrow(contention);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertSame(contention, assertThrows(CannotAcquireLockException.class,
                    () -> service.initialize("Agency", MANILA, ACTOR)));
        } finally {
            TransactionSynchronizationManager.clear();
        }
        verify(transactions).getTransaction(any());
        verify(audit, never()).record(any());
    }

    @Test
    void staleUpdateFailsBeforeMutationOrAudit() {
        AgencyConfigurationEntity entity = new AgencyConfigurationEntity(
                PublicId.of(UUID.randomUUID()), "Agency", MANILA, Instant.EPOCH);
        when(repository.findById((byte) 1)).thenReturn(Optional.of(entity));
        assertThrows(IllegalStateException.class,
                () -> service.update("Other", MANILA, 2, ACTOR));
        verify(repository, never()).flush();
        verify(audit, never()).record(any());
    }

    @Test
    void actorAndZoneMustBeExplicit() {
        assertThrows(NullPointerException.class,
                () -> service.initialize("Agency", MANILA, null));
        assertThrows(NullPointerException.class,
                () -> service.initialize("Agency", null, ACTOR));
    }
}

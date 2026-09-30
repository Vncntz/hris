package io.github.vncntz.hris.platformoperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.BusinessTimeZone;
import io.github.vncntz.hris.sharedkernel.PublicId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgencyConfigurationServiceTest {
    private static final PublicId ACTOR = PublicId.of(UUID.fromString("00000000-0000-0000-0000-000000000007"));
    private static final BusinessTimeZone MANILA = BusinessTimeZone.of("Asia/Manila");
    private final AgencyConfigurationRepository repository = mock(AgencyConfigurationRepository.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final AgencyConfigurationService service = new AgencyConfigurationService(repository, audit,
            Clock.fixed(Instant.parse("2026-09-30T01:02:03Z"), ZoneOffset.ofHours(9)));

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

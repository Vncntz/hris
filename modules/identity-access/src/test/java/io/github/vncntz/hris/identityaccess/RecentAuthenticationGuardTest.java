package io.github.vncntz.hris.identityaccess;

import java.time.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecentAuthenticationGuardTest {
    final CurrentActor actor = mock(CurrentActor.class);
    final RecentAuthenticationSession session = mock(RecentAuthenticationSession.class);
    final UUID id = UUID.randomUUID();
    final Instant now = Instant.parse("2026-10-03T04:00:00Z");
    final RecentAuthenticationGuard guard = new RecentAuthenticationGuard(actor, session,
            new SecurityPolicy(5, Duration.ofMinutes(15), Duration.ofMinutes(5)), Clock.fixed(now, ZoneOffset.UTC));

    @Test void exactExpiryFutureMismatchAndMissingProofFailClosed() {
        when(actor.requireUserId()).thenReturn(id);
        for (var proof : java.util.List.of(new RecentAuthenticationSession.Proof(id, now.minusSeconds(300)),
                new RecentAuthenticationSession.Proof(id, now.minusSeconds(301)),
                new RecentAuthenticationSession.Proof(id, now.plusNanos(1)),
                new RecentAuthenticationSession.Proof(UUID.randomUUID(), now))) {
            when(session.proof()).thenReturn(Optional.of(proof)); reject();
        }
        when(session.proof()).thenReturn(Optional.empty()); reject();
        when(session.proof()).thenReturn(Optional.of(new RecentAuthenticationSession.Proof(id, now.minusSeconds(299))));
        guard.requireRecentAuthentication();
        when(session.proof()).thenReturn(Optional.of(new RecentAuthenticationSession.Proof(id, now)));
        guard.requireRecentAuthentication();
    }
    @Test void actorIsRequiredBeforeSessionLookup() {
        when(actor.requireUserId()).thenThrow(new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Required"));
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class, guard::requireRecentAuthentication);
        verifyNoInteractions(session);
    }
    @Test void configurationDefaultsAndBounds() {
        assertEquals(Duration.ofMinutes(5), new SecurityPolicy(5, Duration.ofMinutes(15)).reauthenticationWindow());
        for (var duration : java.util.Arrays.asList(null, Duration.ZERO, Duration.ofNanos(-1), Duration.ofMinutes(30).plusNanos(1))) {
            assertThrows(IllegalArgumentException.class, () -> new SecurityPolicy(5, Duration.ofMinutes(15), duration));
        }
        new SecurityPolicy(5, Duration.ofMinutes(15), Duration.ofNanos(1));
        new SecurityPolicy(5, Duration.ofMinutes(15), Duration.ofMinutes(30));
        var configuration = new AuthenticationSecurityConfiguration();
        assertThrows(IllegalArgumentException.class, () -> configuration.securityPolicy(5, Duration.ofMinutes(15), Duration.ZERO));
    }
    void reject() {
        var failure = assertThrows(RecentAuthenticationException.class, guard::requireRecentAuthentication);
        assertEquals(RecentAuthenticationException.Reason.PROOF_REQUIRED, failure.reason()); assertNull(failure.getCause());
    }
}

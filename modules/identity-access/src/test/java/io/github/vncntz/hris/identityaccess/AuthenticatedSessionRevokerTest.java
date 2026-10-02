package io.github.vncntz.hris.identityaccess;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedSessionRevokerTest {
    @Test
    void allPrincipalSnapshotsMatchOnlyPublicUuidAndExpiryIsIdempotent() {
        SessionRegistry registry = new SessionRegistryImpl();
        UUID id = UUID.randomUUID();
        Object[] principals = {new AccountPrincipal(id, "synthetic.self"),
                new AccountPrincipal(id, "synthetic.renamed"),
                new AccountPrincipal(UUID.randomUUID(), "synthetic.self"), "unsupported"};
        var tracked = new java.util.ArrayList<SessionInformation>();
        for (var principal : principals) {
            for (int i = 0; i < 2; i++) {
                String session = UUID.randomUUID().toString();
                registry.registerNewSession(session, principal);
                tracked.add(registry.getSessionInformation(session));
            }
        }
        var revoker = new AuthenticatedSessionRevoker(registry);
        revoker.revoke(id);
        revoker.revoke(id);
        for (int i = 0; i < tracked.size(); i++) { assertEquals(i < 4, tracked.get(i).isExpired()); }
        assertEquals(4, registry.getAllPrincipals().size());
    }

    @Test
    void partialExpiryFailureRemainsBoundedAndAlreadyExpiredSessionsStayRevoked() {
        SessionRegistry registry = mock(SessionRegistry.class);
        UUID id = UUID.randomUUID();
        var principal = new AccountPrincipal(id, "synthetic.self");
        var first = new SessionInformation(principal, UUID.randomUUID().toString(), new java.util.Date());
        var failed = mock(SessionInformation.class);
        when(registry.getAllPrincipals()).thenReturn(List.of(principal));
        when(registry.getAllSessions(principal, false)).thenReturn(List.of(first, failed));
        doThrow(new IllegalStateException("Synthetic private session failure")).when(failed).expireNow();
        var failure = assertThrows(SessionRevocationException.class, () -> new AuthenticatedSessionRevoker(registry).revoke(id));
        assertEquals("Authenticated session revocation failed", failure.getMessage());
        assertNull(failure.getCause());
        assertTrue(first.isExpired());
    }
}

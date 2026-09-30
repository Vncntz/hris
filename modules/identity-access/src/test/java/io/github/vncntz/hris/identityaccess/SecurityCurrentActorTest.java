package io.github.vncntz.hris.identityaccess;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityCurrentActorTest {
    private final CurrentActor actor = new SecurityCurrentActor();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnonymousAndEnforcesSyntheticAuthority() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, actor::requireUserId);
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> actor.requireAuthority("test:approve"));
        assertFalse(actor.hasAuthority("test:approve"));

        UUID publicId = UUID.randomUUID();
        AccountPrincipal principal = new AccountPrincipal(publicId, "synthetic.actor");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority("test:approve"))));
        assertEquals(publicId, actor.requireUserId());
        assertTrue(actor.hasAuthority("test:approve"));
        actor.requireAuthority("test:approve");
        assertThrows(AccessDeniedException.class, () -> actor.requireAuthority("test:reject"));
    }
}

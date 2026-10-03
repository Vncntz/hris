package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountAuthoritiesTest {
    private final Instant now = Instant.parse("2026-01-02T03:04:05Z");
    private final PasswordEncoder encoder = new AuthenticationSecurityConfiguration().passwordEncoder();
    private final String password = UUID.randomUUID().toString();
    private final AccountEntity account = new AccountEntity(UUID.randomUUID(), "synthetic.actor",
            encoder.encode(password), now);
    private final AccountRepository repository = mock(AccountRepository.class);
    private final AccountAuthenticationProvider provider;

    AccountAuthoritiesTest() {
        ReflectionTestUtils.setField(account, "id", 42L);
        when(repository.findByCanonicalLogin("synthetic.actor")).thenReturn(Optional.of(account));
        provider = new AccountAuthenticationProvider(new AccountAuthenticationService(repository, encoder,
                Clock.fixed(now, ZoneOffset.UTC), new SecurityPolicy(5, Duration.ofMinutes(15)),
                new MfaVerifier(new MfaSecrets(""), new MfaPolicy(1, Duration.ofMinutes(10), 10)),
                mock(jakarta.persistence.EntityManager.class)));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void normalAuthenticationDeduplicatesAndOrdersEnabledPermissionsForCurrentActor() {
        RoleEntity first = role("first", true, "test:zeta", "test:approve");
        RoleEntity second = role("second", true, "test:approve");
        RoleEntity disabled = role("disabled", false, "test:reject");
        when(repository.findAssignedRoles(42L)).thenReturn(List.of(second, disabled, first));

        Authentication authenticated = signIn();
        assertEquals(List.of("test:approve", "test:zeta"), keys(authenticated));
        assertNull(authenticated.getCredentials());
        var freshness = (AuthenticationGeneration) authenticated.getDetails();
        assertEquals(java.util.Map.of(first.publicId(), 0L, second.publicId(), 0L,
                disabled.publicId(), 0L), freshness.roles().values());
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        CurrentActor actor = new SecurityCurrentActor();
        assertEquals(account.publicId(), actor.requireUserId());
        assertTrue(actor.hasAuthority("test:approve"));
        actor.requireAuthority("test:approve");
        assertFalse(actor.hasAuthority("test:reject"));
        assertThrows(AccessDeniedException.class, () -> actor.requireAuthority("test:reject"));
    }

    @Test
    void noRolesAndRolesWithoutPermissionsAuthenticateWithoutAuthority() {
        when(repository.findAssignedRoles(42L)).thenReturn(List.of());
        assertTrue(signIn().getAuthorities().isEmpty());
        when(repository.findAssignedRoles(42L)).thenReturn(List.of(role("empty", true)));
        assertTrue(signIn().getAuthorities().isEmpty());
        when(repository.findAssignedRoles(42L)).thenReturn(List.of(role("disabled", false, "test:approve")));
        assertTrue(signIn().getAuthorities().isEmpty());
    }

    @Test
    void authorityChangesRequireNewAuthentication() {
        when(repository.findAssignedRoles(42L)).thenReturn(List.of(role("first", true, "test:approve")));
        Authentication original = signIn();
        when(repository.findAssignedRoles(42L)).thenReturn(List.of());
        assertEquals(List.of("test:approve"), keys(original));
        assertTrue(signIn().getAuthorities().isEmpty());
    }

    @Test
    void failedAndDisabledAccountsNeverLoadAssignments() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("synthetic.actor", UUID.randomUUID().toString())));
        ReflectionTestUtils.setField(account, "enabled", false);
        assertThrows(BadCredentialsException.class, this::signIn);
        verify(repository, never()).findAssignedRoles(42L);
    }

    @Test
    void authorityLookupFailureCannotProduceAuthenticatedToken() {
        when(repository.findAssignedRoles(42L)).thenThrow(new DataAccessResourceFailureException("Synthetic outage"));
        var failure = assertThrows(BadCredentialsException.class, this::signIn);
        org.junit.jupiter.api.Assertions.assertNull(failure.getCause());
    }

    private Authentication signIn() {
        return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.actor", password));
    }

    private static List<String> keys(Authentication authentication) {
        return authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList();
    }

    private static RoleEntity role(String name, boolean enabled, String... keys) {
        RoleEntity role = new RoleEntity(UUID.randomUUID(), name, enabled);
        HashSet<PermissionEntity> permissions = new HashSet<>();
        for (String key : keys) {
            permissions.add(new PermissionEntity(key));
        }
        ReflectionTestUtils.setField(role, "permissions", permissions);
        return role;
    }
}

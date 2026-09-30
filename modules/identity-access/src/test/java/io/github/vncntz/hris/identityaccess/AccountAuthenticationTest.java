package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountAuthenticationTest {
    @Test
    void argon2EncodingIsVersionedAndOneWay() {
        PasswordEncoder encoder = new AuthenticationSecurityConfiguration().passwordEncoder();
        String syntheticPassword = UUID.randomUUID().toString();
        String stored = encoder.encode(syntheticPassword);
        assertTrue(stored.startsWith("{argon2@SpringSecurity_v5_8}$argon2id$"));
        assertNotEquals(syntheticPassword, stored);
        assertTrue(encoder.matches(syntheticPassword, stored));
        assertFalse(encoder.matches(UUID.randomUUID().toString(), stored));
    }

    @Test
    void canonicalizationIsLocaleIndependentAndRejectsUnsupportedCharacters() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("synthetic.login", LoginNames.canonicalize("  SYNTHETIC.LOGIN  "));
        } finally {
            Locale.setDefault(original);
        }
        assertThrows(IllegalArgumentException.class, () -> LoginNames.canonicalize("bad login"));
    }

    @Test
    void failuresLockAndExpireUsingInjectedClockWhileSuccessResetsState() {
        PasswordEncoder encoder = new AuthenticationSecurityConfiguration().passwordEncoder();
        String syntheticPassword = UUID.randomUUID().toString();
        UUID publicId = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-02T03:04:05Z");
        AccountEntity account = new AccountEntity(publicId, "synthetic.login",
                encoder.encode(syntheticPassword), start);
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.findByCanonicalLogin(anyString())).thenAnswer(invocation -> Optional.empty());
        when(repository.findByCanonicalLogin("synthetic.login")).thenReturn(Optional.of(account));
        MutableClock clock = new MutableClock(start);
        AccountAuthenticationService service = new AccountAuthenticationService(repository, encoder,
                clock, new SecurityPolicy(3, Duration.ofMinutes(15)));
        AccountAuthenticationProvider provider = new AccountAuthenticationProvider(service);

        BadCredentialsException unknown = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(attempt("unknown.synthetic", syntheticPassword)));
        BadCredentialsException wrong = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(attempt("SYNTHETIC.LOGIN", UUID.randomUUID().toString())));
        assertEquals(unknown.getMessage(), wrong.getMessage());
        assertFalse(service.authenticate("synthetic.login", UUID.randomUUID().toString()).isPresent());
        assertFalse(service.authenticate("synthetic.login", UUID.randomUUID().toString()).isPresent());
        assertEquals(3, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertFalse(service.authenticate("synthetic.login", syntheticPassword).isPresent());

        clock.advance(Duration.ofMinutes(16));
        AccountPrincipal principal = assertInstanceOf(AccountPrincipal.class,
                provider.authenticate(attempt("SYNTHETIC.LOGIN", syntheticPassword)).getPrincipal());
        assertEquals(publicId, principal.publicId());
        assertEquals("synthetic.login", principal.canonicalLogin());
        assertEquals(0, ReflectionTestUtils.getField(account, "failedAttempts"));
        assertNull(ReflectionTestUtils.getField(account, "lockedUntilUtc"));

        ReflectionTestUtils.setField(account, "enabled", false);
        assertFalse(service.authenticate("synthetic.login", syntheticPassword).isPresent());
    }

    private static UsernamePasswordAuthenticationToken attempt(String login, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(login, password);
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        private MutableClock(Instant start) {
            now = new AtomicReference<>(start);
        }

        void advance(Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}

package io.github.vncntz.hris;

import java.nio.ByteBuffer;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.github.vncntz.hris.identityaccess.AccountAuthenticationProvider;
import io.github.vncntz.hris.identityaccess.AccountPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vaadin.productionMode=true", "hris.security.max-failed-attempts=3",
                "hris.security.lock-duration=PT7M"})
class AccountAuthenticationIT {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AccountAuthenticationProvider provider;

    @Autowired
    private TestClock clock;

    @Value("${local.server.port}")
    private int port;

    @Test
    void eachFailedHttpSubmissionCountsOnceAndLocksAtExactThreshold() throws Exception {
        String login = "synthetic." + UUID.randomUUID().toString().substring(0, 8);
        String password = UUID.randomUUID().toString();
        Instant start = Instant.parse("2026-01-02T03:04:05Z");
        clock.set(start);
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.ofInstant(start, ZoneOffset.UTC));
        jdbc.update("INSERT INTO identity_account "
                        + "(public_id, canonical_login, password_hash, enabled, failed_attempts, "
                        + "credential_updated_at_utc, security_updated_at_utc, row_version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                uuidBytes(UUID.randomUUID()), login, encoder.encode(password), true, 0, timestamp, timestamp, 0);
        for (int attempt = 1; attempt <= 3; attempt++) {
            verifyHttpFailure(login, UUID.randomUUID().toString());
            assertEquals(attempt, jdbc.queryForObject(
                    "SELECT failed_attempts FROM identity_account WHERE canonical_login = ?", Integer.class, login));
            Timestamp expiry = jdbc.queryForObject(
                    "SELECT locked_until_utc FROM identity_account WHERE canonical_login = ?", Timestamp.class, login);
            if (attempt < 3) {
                assertNull(expiry);
            } else {
                assertEquals(Timestamp.valueOf(LocalDateTime.ofInstant(start.plusSeconds(420), ZoneOffset.UTC)), expiry);
            }
        }
        verifyHttpFailure(login, password);
        verifyHttpFailure(login, UUID.randomUUID().toString());
        assertEquals(3, jdbc.queryForObject(
                "SELECT failed_attempts FROM identity_account WHERE canonical_login = ?", Integer.class, login));
        assertEquals(Timestamp.valueOf(LocalDateTime.ofInstant(start.plusSeconds(420), ZoneOffset.UTC)),
                jdbc.queryForObject("SELECT locked_until_utc FROM identity_account WHERE canonical_login = ?",
                        Timestamp.class, login));
        clock.set(start.plusSeconds(420));
        verifyHttpLogin(login, password);
        assertEquals(0, jdbc.queryForObject(
                "SELECT failed_attempts FROM identity_account WHERE canonical_login = ?", Integer.class, login));
        assertNull(jdbc.queryForObject("SELECT locked_until_utc FROM identity_account WHERE canonical_login = ?",
                Timestamp.class, login));
        verifyHttpFailure("absent." + login, password);
        verifyHttpFailure("bad login", password);
        jdbc.update("UPDATE identity_account SET enabled = 0 WHERE canonical_login = ?", login);
        verifyHttpFailure(login, password);
        verifyHttpFailure(login, UUID.randomUUID().toString());
        assertEquals(0, jdbc.queryForObject(
                "SELECT failed_attempts FROM identity_account WHERE canonical_login = ?", Integer.class, login));
        assertNull(jdbc.queryForObject("SELECT locked_until_utc FROM identity_account WHERE canonical_login = ?",
                Timestamp.class, login));
    }

    private void verifyHttpFailure(String login, String password) throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        try (HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
            URI base = URI.create("http://127.0.0.1:" + port);
            assertEquals(200, client.send(HttpRequest.newBuilder(base.resolve("/login")).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
            String form = "username=" + URLEncoder.encode(login, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
            HttpResponse<Void> rejected = client.send(HttpRequest.newBuilder(base.resolve("/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.discarding());
            assertEquals(302, rejected.statusCode());
            assertTrue(rejected.headers().firstValue("Location").orElseThrow().endsWith("/login?error"));
            assertEquals(302, client.send(HttpRequest.newBuilder(base.resolve("/")).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }

    @Test
    void authenticationStateRoundTripsThroughMigratedMySql() throws Exception {
        assertTrue(mysql.isRunning());
        assertEquals(9, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class));

        String login = "synthetic." + UUID.randomUUID().toString().substring(0, 8);
        String syntheticPassword = UUID.randomUUID().toString();
        UUID publicId = UUID.randomUUID();
        String encoded = encoder.encode(syntheticPassword);
        Instant start = Instant.parse("2026-01-02T03:04:05Z");
        clock.set(start);
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.ofInstant(start, ZoneOffset.UTC));
        jdbc.update("INSERT INTO identity_account "
                        + "(public_id, canonical_login, password_hash, enabled, failed_attempts, "
                        + "credential_updated_at_utc, security_updated_at_utc, row_version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                uuidBytes(publicId), login, encoded, true, 0, timestamp, timestamp, 0);

        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO identity_account (public_id, canonical_login, password_hash, enabled, "
                        + "failed_attempts, credential_updated_at_utc, security_updated_at_utc, row_version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                uuidBytes(UUID.randomUUID()), login.toUpperCase(java.util.Locale.ROOT), encoded,
                true, 0, timestamp, timestamp, 0));

        String stored = jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login = ?",
                String.class, login);
        assertNotEquals(syntheticPassword, stored);
        assertTrue(stored.startsWith("{argon2@SpringSecurity_v5_8}$argon2id$"));
        assertEquals(0, jdbc.queryForObject("SELECT failed_attempts FROM identity_account WHERE canonical_login = ?",
                Integer.class, login));

        AccountPrincipal initial = assertInstanceOf(AccountPrincipal.class,
                provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                        login.toUpperCase(java.util.Locale.ROOT), syntheticPassword)).getPrincipal());
        assertEquals(publicId, initial.publicId());
        verifyHttpLogin(login, syntheticPassword);

        assertBadCredentials("absent." + login, syntheticPassword);
        for (int i = 0; i < 3; i++) {
            assertBadCredentials(login, UUID.randomUUID().toString());
        }
        assertEquals(3, jdbc.queryForObject("SELECT failed_attempts FROM identity_account WHERE canonical_login = ?",
                Integer.class, login));
        assertNotNull(jdbc.queryForObject("SELECT locked_until_utc FROM identity_account WHERE canonical_login = ?",
                Timestamp.class, login));
        assertBadCredentials(login, syntheticPassword);

        clock.set(start.plus(Duration.ofMinutes(16)));
        AccountPrincipal principal = assertInstanceOf(AccountPrincipal.class,
                provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                        login.toUpperCase(java.util.Locale.ROOT), syntheticPassword)).getPrincipal());
        assertEquals(publicId, principal.publicId());
        assertEquals(login, principal.canonicalLogin());
        assertEquals(0, jdbc.queryForObject("SELECT failed_attempts FROM identity_account WHERE canonical_login = ?",
                Integer.class, login));
        assertNull(jdbc.queryForObject("SELECT locked_until_utc FROM identity_account WHERE canonical_login = ?",
                Timestamp.class, login));

        jdbc.update("UPDATE identity_account SET enabled = 0 WHERE canonical_login = ?", login);
        assertBadCredentials(login, syntheticPassword);
    }

    private void assertBadCredentials(String login, String password) {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(login, password)));
    }

    private void verifyHttpLogin(String login, String password) throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        try (HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
            URI base = URI.create("http://127.0.0.1:" + port);
            HttpResponse<Void> loginPage = client.send(HttpRequest.newBuilder(base.resolve("/login"))
                    .GET().build(), HttpResponse.BodyHandlers.discarding());
            assertEquals(200, loginPage.statusCode());

            String form = "username=" + URLEncoder.encode(login, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
            HttpResponse<Void> signedIn = client.send(HttpRequest.newBuilder(base.resolve("/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(302, signedIn.statusCode());
            HttpResponse<Void> protectedRoot = client.send(HttpRequest.newBuilder(base.resolve("/"))
                    .GET().build(), HttpResponse.BodyHandlers.discarding());
            assertEquals(200, protectedRoot.statusCode());
        }
    }

    private static byte[] uuidBytes(UUID value) {
        return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits()).array();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {
        @Bean
        @Primary
        TestClock authenticationTestClock() {
            return new TestClock();
        }
    }

    static final class TestClock extends Clock {
        private final AtomicReference<Instant> current = new AtomicReference<>(Instant.EPOCH);

        void set(Instant instant) {
            current.set(instant);
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
            return current.get();
        }
    }
}

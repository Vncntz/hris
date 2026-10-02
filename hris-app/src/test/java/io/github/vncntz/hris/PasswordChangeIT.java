package io.github.vncntz.hris;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import io.github.vncntz.hris.identityaccess.*;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vaadin.productionMode=true", "logging.level.root=OFF"})
@Import({LocalProvisioningIdentityConfiguration.class, PasswordChangeIT.ProbeConfiguration.class})
class PasswordChangeIT {
    @Container @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    private static final Instant NOW = Instant.parse("2026-10-02T04:05:06.123456Z");
    @Autowired private PasswordChangeService service;
    @Autowired private FirstAdministratorProvisioner bootstrap;
    @Autowired private AccountCreationService creation;
    @Autowired private AccountAuthenticationProvider provider;
    @Autowired private CurrentActor actor;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Flyway flyway;
    @Autowired private SessionRegistry registry;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private ProbeCredentials probe;
    @MockitoSpyBean private AuditRecorder audit;
    @MockitoSpyBean private PasswordEncoder encoder;
    @MockitoSpyBean private AuthenticatedSessionRevoker revoker;
    @Value("${local.server.port}") private int port;
    private UUID id;
    private Authentication authenticated;
    private String oldPassword;

    @BeforeEach
    void freshSyntheticIdentity() {
        for (Object principal : registry.getAllPrincipals()) {
            for (var session : registry.getAllSessions(principal, true)) {
                registry.removeSessionInformation(session.getSessionId());
            }
        }
        Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .cleanDisabled(false).load().clean();
        flyway.migrate();
        oldPassword = UUID.randomUUID().toString();
        id = bootstrap.provision("synthetic.self", oldPassword.toCharArray(), oldPassword.toCharArray());
        authenticated = authenticate(oldPassword);
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        clearInvocations(audit, encoder, revoker);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        probe.clear();
    }

    @Test
    void successfulReplacementPreservesAssignmentsBootstrapUnrelatedAccountAndSchema() {
        String otherPassword = UUID.randomUUID().toString();
        var other = creation.create("synthetic.other", otherPassword.toCharArray(), otherPassword.toCharArray());
        String otherBefore = snapshot("synthetic.other");
        jdbc.update("UPDATE identity_account SET failed_attempts=5,locked_until_utc=?,credential_updated_at_utc=? WHERE canonical_login='synthetic.self'",
                Timestamp.from(NOW.plusSeconds(900)), Timestamp.from(Instant.EPOCH));
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return call.callRealMethod();
        }).when(encoder).matches(any(), any());
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return call.callRealMethod();
        }).when(encoder).encode(any());
        String replacement = "  " + UUID.randomUUID() + "  ";
        change(replacement);
        reset(encoder);
        var state = jdbc.queryForMap("SELECT public_id,canonical_login,enabled,failed_attempts,locked_until_utc,"
                + "credential_updated_at_utc,security_updated_at_utc FROM identity_account WHERE canonical_login='synthetic.self'");
        assertEquals("synthetic.self", state.get("canonical_login"));
        assertEquals(Boolean.TRUE, state.get("enabled"));
        assertEquals(0, ((Number)state.get("failed_attempts")).intValue());
        assertNull(state.get("locked_until_utc"));
        assertEquals(java.time.LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), state.get("credential_updated_at_utc"));
        assertEquals(java.time.LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), state.get("security_updated_at_utc"));
        assertTrue(otherBefore.equals(snapshot("synthetic.other")));
        assertEvent(1);
        assertPreserved(2, 3);
        assertThrows(BadCredentialsException.class, () -> authenticate(oldPassword));
        var fresh = authenticate(replacement);
        assertEquals(id, ((AccountPrincipal)fresh.getPrincipal()).publicId());
        assertEquals(authenticated.getAuthorities(), fresh.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(fresh);
        actor.requireAuthority("identity:admin");
        var unrelated = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.other", otherPassword));
        assertEquals(other.publicId(), ((AccountPrincipal)unrelated.getPrincipal()).publicId());
        assertTrue(unrelated.getAuthorities().isEmpty());
        flyway.validate();
        assertEquals(6, count("flyway_schema_history"));
    }

    @Test
    void ordinaryZeroAuthorityAccountChangesOnlyItsOwnCredential() {
        String ordinaryPassword = UUID.randomUUID().toString();
        UUID ordinary = creation.create("synthetic.ordinary", ordinaryPassword.toCharArray(), ordinaryPassword.toCharArray()).publicId();
        Authentication signedIn = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                "synthetic.ordinary", ordinaryPassword));
        assertTrue(signedIn.getAuthorities().isEmpty());
        String administratorBefore = snapshot("synthetic.self");
        SecurityContextHolder.getContext().setAuthentication(signedIn);
        String replacement = UUID.randomUUID().toString();
        char[][] buffers = {ordinaryPassword.toCharArray(), replacement.toCharArray(), replacement.toCharArray()};
        invoke(buffers);
        cleared(buffers);
        assertTrue(administratorBefore.equals(snapshot("synthetic.self")));
        var fresh = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.ordinary", replacement));
        assertEquals(ordinary, ((AccountPrincipal)fresh.getPrincipal()).publicId());
        assertTrue(fresh.getAuthorities().isEmpty());
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("synthetic.ordinary", ordinaryPassword)));
        var event = jdbc.queryForMap("SELECT actor_reference,target_reference FROM audit_event WHERE action='IDENTITY_PASSWORD_CHANGED'");
        assertEquals(ordinary.toString(), event.get("actor_reference"));
        assertEquals(ordinary.toString(), event.get("target_reference"));
        assertPreserved(2, 3);
    }

    @Test
    void realActorDenialWrongCurrentAndAmbientTransactionPreserveCredentialAndAudit() {
        SecurityContextHolder.clearContext();
        char[][] denied = buffers(UUID.randomUUID().toString());
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> invoke(denied));
        cleared(denied);
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        char[][] wrong = buffers(UUID.randomUUID().toString());
        java.util.Arrays.fill(wrong[0], ' ');
        bounded(PasswordChangeException.Reason.CURRENT_CREDENTIAL_REJECTED, () -> invoke(wrong));
        cleared(wrong);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            char[][] nested = buffers(UUID.randomUUID().toString());
            bounded(PasswordChangeException.Reason.PERSISTENCE_FAILED, () -> invoke(nested));
            cleared(nested);
        });
        verify(encoder, never()).encode(any());
        verifyNoInteractions(audit, revoker);
        assertPreserved(1, 1);
        authenticate(oldPassword);
    }

    @Test
    void auditInsertThenFailureRollsBackPasswordAndAuditWithoutExpiry() {
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("Synthetic audit failure");
        }).when(audit).record(any());
        assertRollback(PasswordChangeException.Reason.PERSISTENCE_FAILED);
        verifyNoInteractions(revoker);
    }

    @Test
    void databaseFlushFailureRollsBackAuditAndCredentialBeforeExpiry() {
        jdbc.execute("CREATE TRIGGER synthetic_password_failure BEFORE UPDATE ON identity_account FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic mutation failure'");
        try {
            assertRollback(PasswordChangeException.Reason.PERSISTENCE_FAILED);
            verifyNoInteractions(revoker);
        } finally { jdbc.execute("DROP TRIGGER synthetic_password_failure"); }
    }

    @Test
    void partialSessionMarkingFailureRollsBackFlushedCredentialAndAudit() {
        SessionInformation session = track();
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("Synthetic session marking failure");
        }).when(revoker).revoke(id);
        // The service sanitizes infrastructure failures even after some local sessions expired.
        assertRollback(PasswordChangeException.Reason.SESSION_REVOCATION_FAILED);
        assertTrue(session.isExpired());
    }

    @Test
    void laterCommitFailureLeavesExpiredSessionsButOldCredentialAndNoChangeAudit() {
        SessionInformation session = track();
        doAnswer(call -> {
            call.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("Synthetic later commit failure");
                }
            });
            return null;
        }).when(revoker).revoke(id);
        assertRollback(PasswordChangeException.Reason.PERSISTENCE_FAILED);
        assertTrue(session.isExpired());
        authenticate(oldPassword);
    }

    @Test
    void concurrentVerifiedSnapshotsCommitOneChangeAndRejectStaleLoserWithoutRetry() throws Exception {
        CountDownLatch bothVerified = new CountDownLatch(1), winnerCommitted = new CountDownLatch(1);
        AtomicInteger encodingOrder = new AtomicInteger();
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            if (encodingOrder.getAndIncrement() == 0) {
                if (!bothVerified.await(15, TimeUnit.SECONDS)) { throw new IllegalStateException("Synchronization timeout"); }
            } else {
                bothVerified.countDown();
                if (!winnerCommitted.await(15, TimeUnit.SECONDS)) { throw new IllegalStateException("Synchronization timeout"); }
            }
            return call.callRealMethod();
        }).when(encoder).encode(any());
        String winnerPassword = UUID.randomUUID().toString(), loserPassword = UUID.randomUUID().toString();
        try (var workers = Executors.newFixedThreadPool(2)) {
            var winner = workers.submit(() -> {
                try { return attempt(winnerPassword); }
                finally { winnerCommitted.countDown(); }
            });
            // Wait for the first request to finish verification and reach encoding.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (encodingOrder.get() == 0 && System.nanoTime() < deadline) { Thread.yield(); }
            assertEquals(1, encodingOrder.get());
            var loser = workers.submit(() -> attempt(loserPassword));
            assertNull(winner.get(20, TimeUnit.SECONDS));
            assertEquals(PasswordChangeException.Reason.STALE_CREDENTIAL, loser.get(20, TimeUnit.SECONDS));
        } finally { bothVerified.countDown(); winnerCommitted.countDown(); }
        verify(encoder, times(2)).matches(any(), any());
        verify(audit, times(1)).record(any());
        verify(revoker, times(1)).revoke(id);
        reset(encoder);
        assertPreserved(1, 2);
        assertEvent(1);
        authenticate(winnerPassword);
        assertThrows(BadCredentialsException.class, () -> authenticate(loserPassword));
        assertThrows(BadCredentialsException.class, () -> authenticate(oldPassword));
    }

    @Test
    void servletBoundManagedSnapshotCannotHidePersistedDisablement() {
        var entities = entityManagerFactory.createEntityManager();
        try {
            // Model a request persistence context retained across transactions, without
            // exposing the internal entity outside this persistence-focused test.
            entities.createQuery("select account from AccountEntity account where account.publicId=:id")
                    .setParameter("id", id).getSingleResult();
            TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entities));
            // Permit the initial enabled scalar read, then disable after verification while the
            // preloaded entity still says enabled. Refresh under the write lock must see the change.
            doAnswer(call -> {
                jdbc.update("UPDATE identity_account SET enabled=0 WHERE canonical_login='synthetic.self'");
                return call.callRealMethod();
            }).when(encoder).encode(any());
            char[][] buffers = buffers(UUID.randomUUID().toString());
            bounded(PasswordChangeException.Reason.ACCOUNT_UNAVAILABLE, () -> invoke(buffers));
            cleared(buffers);
            assertEquals(0, jdbc.queryForObject("SELECT enabled FROM identity_account WHERE canonical_login='synthetic.self'", Integer.class));
            assertPreserved(1, 1);
            verifyNoInteractions(audit, revoker);
        } finally {
            TransactionSynchronizationManager.unbindResourceIfPossible(entityManagerFactory);
            entities.close();
        }
    }

    @Test
    void enabledStateIsRecheckedAfterEncodingBeforeMutation() {
        doAnswer(call -> {
            jdbc.update("UPDATE identity_account SET enabled=0 WHERE canonical_login='synthetic.self'");
            return call.callRealMethod();
        }).when(encoder).encode(any());
        String before = encoding();
        char[][] buffers = buffers(UUID.randomUUID().toString());
        bounded(PasswordChangeException.Reason.ACCOUNT_UNAVAILABLE, () -> invoke(buffers));
        cleared(buffers);
        assertTrue(before.equals(encoding()));
        assertPreserved(1, 1);
        verifyNoInteractions(audit, revoker);
    }

    @Test
    void realHttpMultipleSessionsCallerExpiryUnrelatedSessionFreshLoginCsrfFixationAndLogout() throws Exception {
        String otherPassword = UUID.randomUUID().toString();
        UUID other = creation.create("synthetic.other", otherPassword.toCharArray(), otherPassword.toCharArray()).publicId();
        try (Browser first = new Browser(); Browser second = new Browser(); Browser third = new Browser();
                Browser unrelated = new Browser(); Browser fresh = new Browser(); Browser old = new Browser()) {
            first.login("synthetic.self", oldPassword, true);
            second.login("synthetic.self", oldPassword, true);
            third.login("synthetic.self", oldPassword, true);
            unrelated.login("synthetic.other", otherPassword, true);
            var own = tracked(id);
            assertEquals(3, own.size());
            assertEquals(1, tracked(other).size());
            for (var session : own) { assertFalse(session.isExpired()); }
            String replacement = UUID.randomUUID().toString();
            char[][] buffers = buffers(replacement);
            probe.next.set(buffers);
            assertEquals(403, first.post("/test/change", null).statusCode());
            assertSame(buffers, probe.next.get());
            assertEquals(204, first.post("/test/change", first.get("/test/csrf").body()).statusCode());
            cleared(buffers);
            for (var session : own) { assertTrue(session.isExpired()); }
            for (Browser browser : List.of(first, second, third)) {
                assertEquals(302, browser.get("/test/session").statusCode());
                assertEquals(302, browser.get("/test/session").statusCode());
            }
            assertEquals(0, tracked(id).size()); // real servlet destruction publishes registry cleanup
            assertEquals(200, unrelated.get("/test/session").statusCode());
            assertEquals(1, tracked(other).size());
            old.login("synthetic.self", oldPassword, false);
            fresh.login("synthetic.self", replacement, true);
            assertEquals(1, tracked(id).size());
            String csrf = fresh.get("/test/csrf").body();
            assertEquals(302, fresh.post("/logout", csrf).statusCode());
            assertEquals(302, fresh.get("/test/session").statusCode());
            assertEquals(0, tracked(id).size());
            assertEquals(200, unrelated.get("/test/session").statusCode());
            assertEvent(1);
        }
    }

    private PasswordChangeException.Reason attempt(String replacement) {
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        try { change(replacement); return null; }
        catch (PasswordChangeException failure) { return failure.reason(); }
        finally { SecurityContextHolder.clearContext(); }
    }

    private void assertRollback(PasswordChangeException.Reason reason) {
        String before = snapshot("synthetic.self");
        char[][] buffers = buffers(UUID.randomUUID().toString());
        bounded(reason, () -> invoke(buffers));
        cleared(buffers);
        assertTrue(before.equals(snapshot("synthetic.self")),
                "Failed mutation must preserve every account column");
        assertPreserved(1, 1);
    }

    private void change(String replacement) {
        char[][] buffers = buffers(replacement);
        try { invoke(buffers); } finally { cleared(buffers); }
    }

    private char[][] buffers(String replacement) {
        return new char[][]{oldPassword.toCharArray(), replacement.toCharArray(), replacement.toCharArray()};
    }

    private void invoke(char[][] buffers) { service.change(buffers[0], buffers[1], buffers[2]); }
    private Authentication authenticate(String password) {
        return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.self", password));
    }
    private String encoding() {
        return jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login='synthetic.self'", String.class);
    }
    private String snapshot(String login) {
        return jdbc.queryForMap("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,"
                + "locked_until_utc,credential_updated_at_utc,security_updated_at_utc,row_version "
                + "FROM identity_account WHERE canonical_login=?", login).toString();
    }
    private SessionInformation track() {
        String session = UUID.randomUUID().toString();
        registry.registerNewSession(session, authenticated.getPrincipal());
        return registry.getSessionInformation(session);
    }
    private List<SessionInformation> tracked(UUID publicId) {
        return registry.getAllPrincipals().stream()
                .filter(value -> value instanceof AccountPrincipal account && publicId.equals(account.publicId()))
                .flatMap(value -> registry.getAllSessions(value, true).stream()).toList();
    }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private void assertPreserved(int accounts, int events) {
        assertEquals(accounts, count("identity_account")); assertEquals(events, count("audit_event"));
        for (String table : List.of("identity_role", "identity_permission", "identity_account_role", "identity_role_permission")) {
            assertEquals(1, count(table));
        }
        assertEquals(1, jdbc.queryForObject("SELECT completed FROM identity_bootstrap_state", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='FIRST_ADMINISTRATOR_PROVISIONED'", Integer.class));
        flyway.validate();
        assertEquals(6, count("flyway_schema_history"));
    }
    private void assertEvent(int expected) {
        assertEquals(expected, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_PASSWORD_CHANGED'", Integer.class));
        var event = jdbc.queryForMap("SELECT actor_reference,target_type,target_reference,reason,context FROM audit_event WHERE action='IDENTITY_PASSWORD_CHANGED'");
        assertEquals(id.toString(), event.get("actor_reference"));
        assertEquals(id.toString(), event.get("target_reference"));
        assertEquals("IDENTITY_ACCOUNT", event.get("target_type"));
        assertEquals("self-service-password-change", event.get("context"));
        assertNull(event.get("reason"));
        assertFalse(event.toString().contains(oldPassword));
        assertFalse(event.toString().contains(encoding()));
    }
    private static void bounded(PasswordChangeException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(PasswordChangeException.class, action);
        assertEquals(reason, failure.reason());
        assertNull(failure.getCause());
        assertEquals("Password change failed: " + reason, failure.getMessage());
    }
    private static void cleared(char[]... buffers) {
        for (char[] buffer : buffers) {
            boolean empty = true;
            for (char value : buffer) { empty &= value == 0; }
            assertTrue(empty, "Owned credential buffer must be cleared");
        }
    }

    /** Test-only transport; uses the production chain and derives identity on its servlet thread. */
    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean static BeanPostProcessor probeRouteAuthorization() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof HttpSecurity http) {
                        // Permit only authenticated test probes before Vaadin's default deny rule.
                        // All production authentication, CSRF, fixation, expiry and logout filters remain.
                        try {
                            http.authorizeHttpRequests(authorize -> authorize.requestMatchers(
                                    request -> request.getRequestURI().startsWith("/test/")).authenticated());
                        } catch (Exception failure) { throw new IllegalStateException("Test route configuration failed"); }
                    }
                    return bean;
                }
            };
        }
        @Bean @Primary Clock passwordChangeTestClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean ProbeCredentials probeCredentials() { return new ProbeCredentials(); }
        @Bean ServletRegistrationBean<HttpServlet> passwordChangeProbe(
                PasswordChangeService service, CurrentActor actor, ProbeCredentials probe) {
            return new ServletRegistrationBean<>(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws java.io.IOException {
                    actor.requireUserId();
                    if (request.getPathInfo().equals("/csrf")) {
                        response.getWriter().write(((CsrfToken)request.getAttribute(CsrfToken.class.getName())).getToken());
                    } else {
                        if (request.getSession().getMaxInactiveInterval() != 1800) { response.setStatus(500); return; }
                        response.getWriter().write("authenticated");
                    }
                }
                @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                    char[][] buffers = probe.next.getAndSet(null);
                    if (buffers == null) { response.setStatus(400); return; }
                    service.change(buffers[0], buffers[1], buffers[2]);
                    response.setStatus(204);
                }
            }, "/test/*");
        }
    }

    static final class ProbeCredentials {
        final AtomicReference<char[][]> next = new AtomicReference<>();
        void clear() {
            char[][] buffers = next.getAndSet(null);
            if (buffers != null) { for (char[] buffer : buffers) { java.util.Arrays.fill(buffer, '\0'); } }
        }
    }

    private final class Browser implements AutoCloseable {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> post(String path, String csrf) throws Exception {
            var request = HttpRequest.newBuilder(uri(path));
            if (csrf != null) { request.header("X-CSRF-TOKEN", csrf); }
            return client.send(request.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        }
        void login(String login, String password, boolean success) throws Exception {
            assertEquals(200, get("/login").statusCode());
            String before = sessionCookie();
            assertFalse(before.isBlank(), "Login page must establish a session before authentication");
            String form = "username=" + URLEncoder.encode(login, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
            var response = client.send(HttpRequest.newBuilder(uri("/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.discarding());
            assertEquals(302, response.statusCode());
            if (success) {
                assertFalse(before.equals(sessionCookie()), "Authentication must change the servlet session identifier");
                assertEquals(200, get("/test/session").statusCode());
            } else {
                assertTrue(response.headers().firstValue("location").orElse("").contains("error"));
                assertEquals(302, get("/test/session").statusCode());
            }
        }
        String sessionCookie() {
            return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("JSESSIONID"))
                    .map(java.net.HttpCookie::getValue).findFirst().orElse("");
        }
        URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
        @Override public void close() { client.close(); }
    }
}

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
@Import({LocalProvisioningIdentityConfiguration.class, AccountLifecycleIT.ProbeConfiguration.class})
class AccountLifecycleIT {
    @Container @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    private static final Instant NOW = Instant.parse("2026-10-02T04:05:06.123456Z");
    @Autowired private AccountLifecycleService service;
    @Autowired private FirstAdministratorProvisioner bootstrap;
    @Autowired private AccountCreationService creation;
    @MockitoSpyBean private AccountAuthenticationProvider provider;
    @Autowired private CurrentActor actor;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Flyway flyway;
    @MockitoSpyBean private SessionRegistry registry;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @MockitoSpyBean private AccountSessionRegistrationService registration;
    @MockitoSpyBean private AuditRecorder audit;
    @MockitoSpyBean private AuthenticatedSessionRevoker revoker;
    @Value("${local.server.port}") private int port;
    private UUID id;
    private Authentication authenticated;
    private String oldPassword;
    private UUID target;
    private String targetPassword;

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
        authenticated = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.self", oldPassword));
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        targetPassword = UUID.randomUUID().toString();
        target = creation.create("synthetic.target", targetPassword.toCharArray(), targetPassword.toCharArray()).publicId();
        clearInvocations(audit, revoker);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void httpDisableExpiresEveryTargetSessionPreservesOthersAndEnableClearsLockWithUnchangedCredential() throws Exception {
        jdbc.update("INSERT INTO identity_account_role SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r "
                + "WHERE a.canonical_login='synthetic.target'");
        String invariant = invariant();
        String assignments = assignments();
        try (Browser first = new Browser(); Browser second = new Browser(); Browser third = new Browser();
                Browser administrator = new Browser(); Browser rejected = new Browser(); Browser restored = new Browser()) {
            for (Browser browser : List.of(first, second, third)) { browser.login("synthetic.target", targetPassword, true); }
            administrator.login("synthetic.self", oldPassword, true);
            String administratorBefore = administratorSnapshot();
            var prior = tracked(target);
            assertEquals(3, prior.size());
            var adminSessions = tracked(id);
            String csrf = administrator.get("/test/csrf").body();
            assertEquals(403, administrator.post("/test/disable?target=" + target, null).statusCode());
            assertEquals(0, generation());
            assertEquals(204, administrator.post("/test/disable?target=" + target, csrf).statusCode());
            assertEquals(1, generation());
            assertEquals(0, enabled());
            for (var session : prior) { assertTrue(session.isExpired()); }
            for (var session : adminSessions) { assertFalse(session.isExpired()); }
            for (Browser browser : List.of(first, second, third)) { assertEquals(302, browser.get("/test/session").statusCode()); }
            assertTrue(tracked(target).isEmpty());
            assertEquals(200, administrator.get("/test/session").statusCode());
            rejected.login("synthetic.target", targetPassword, false);
            jdbc.update("UPDATE identity_account SET failed_attempts=5,locked_until_utc=? WHERE canonical_login='synthetic.target'",
                    Timestamp.from(NOW.plusSeconds(900)));
            // Even a stale tracked entry inserted while disabled is expired by enable.
            registry.registerNewSession(UUID.randomUUID().toString(), new AccountPrincipal(target, "synthetic.target"));
            var stale = tracked(target).getFirst();
            assertEquals(204, administrator.post("/test/enable?target=" + target, csrf).statusCode());
            assertTrue(stale.isExpired());
            assertEquals(2, generation());
            assertEquals(1, enabled());
            assertEquals(0, jdbc.queryForObject("SELECT failed_attempts FROM identity_account WHERE canonical_login='synthetic.target'", Integer.class));
            assertNull(jdbc.queryForObject("SELECT locked_until_utc FROM identity_account WHERE canonical_login='synthetic.target'", Object.class));
            assertTrue(invariant.equals(invariant()));
            assertEquals(assignments, assignments());
            restored.login("synthetic.target", targetPassword, true);
            assertEquals(200, administrator.get("/test/session").statusCode());
            assertTrue(administratorBefore.equals(administratorSnapshot()));
            assertEvents(1, 1);
        }
    }

    @Test
    void realActorDenialSelfDisableMissingNoOpAndAmbientTransactionLeaveNoLifecycleAudit() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service.disable(target));
        SecurityContextHolder.getContext().setAuthentication(provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("synthetic.target", targetPassword)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.disable(id));
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        bounded(AccountLifecycleException.Reason.SELF_DISABLE_REJECTED, () -> service.disable(id));
        bounded(AccountLifecycleException.Reason.ACCOUNT_UNAVAILABLE, () -> service.disable(UUID.randomUUID()));
        bounded(AccountLifecycleException.Reason.ALREADY_ENABLED, () -> service.enable(target));
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                bounded(AccountLifecycleException.Reason.PERSISTENCE_FAILED, () -> service.disable(target)));
        assertEquals(0, generation());
        assertEquals(1, enabled());
        verifyNoInteractions(audit, revoker);
        service.disable(target);
        clearInvocations(audit, revoker);
        bounded(AccountLifecycleException.Reason.ALREADY_DISABLED, () -> service.disable(target));
        verifyNoInteractions(audit, revoker);
        assertEvents(1, 0);
    }

    @Test
    void auditFailureRollsBackWithoutRevocation() {
        String before = snapshot();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("Synthetic audit failure"); }).when(audit).record(any());
        bounded(AccountLifecycleException.Reason.PERSISTENCE_FAILED, () -> service.disable(target));
        assertTrue(before.equals(snapshot()));
        assertEvents(0, 0);
        verifyNoInteractions(revoker);
    }

    @Test
    void mutationFlushFailureRollsBackAuditBeforeRevocation() {
        String before = snapshot();
        jdbc.execute("CREATE TRIGGER synthetic_lifecycle_failure BEFORE UPDATE ON identity_account FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic mutation failure'");
        try {
            bounded(AccountLifecycleException.Reason.PERSISTENCE_FAILED, () -> service.disable(target));
            assertTrue(before.equals(snapshot()));
            assertEvents(0, 0);
            verifyNoInteractions(revoker);
        } finally { jdbc.execute("DROP TRIGGER synthetic_lifecycle_failure"); }
    }

    @Test
    void revocationFailureRollsBackBothLifecycleDirectionsAfterFlush() {
        for (boolean enable : List.of(false, true)) {
            if (enable) { reset(revoker); service.disable(target); }
            String before = snapshot();
            SessionInformation session = trackTarget();
            doAnswer(call -> {
                call.callRealMethod();
                throw new IllegalStateException("Synthetic expiry failure");
            }).when(revoker).revoke(target);
            bounded(AccountLifecycleException.Reason.SESSION_REVOCATION_FAILED,
                    () -> { if (enable) { service.enable(target); } else { service.disable(target); } });
            assertTrue(before.equals(snapshot()));
            assertTrue(session.isExpired());
            assertEvents(enable ? 1 : 0, 0);
        }
    }

    @Test
    void lateCommitFailureRetainsPreviousDatabaseStateWhileSessionsRemainExpiredForBothDirections() {
        for (boolean enable : List.of(false, true)) {
            if (enable) { reset(revoker); service.disable(target); }
            String before = snapshot();
            SessionInformation session = trackTarget();
            doAnswer(call -> {
                call.callRealMethod();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) {
                        throw new IllegalStateException("Synthetic later commit failure");
                    }
                });
                return null;
            }).when(revoker).revoke(target);
            bounded(AccountLifecycleException.Reason.PERSISTENCE_FAILED,
                    () -> { if (enable) { service.enable(target); } else { service.disable(target); } });
            assertTrue(before.equals(snapshot()));
            assertTrue(session.isExpired());
            assertEvents(enable ? 1 : 0, 0);
            if (!enable) {
                assertTrue(provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.target", targetPassword)).isAuthenticated());
            } else {
                assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated("synthetic.target", targetPassword)));
            }
        }
    }

    @Test
    void servletBoundStaleEnabledSnapshotIsRefreshedUnderLifecycleLock() {
        var entities = entityManagerFactory.createEntityManager();
        try {
            entities.createQuery("select account from AccountEntity account where account.publicId=:id")
                    .setParameter("id", target).getSingleResult();
            service.disable(target);
            TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entities));
            service.enable(target);
            assertEquals(1, enabled());
            assertEquals(2, generation());
            assertEvents(1, 1);
        } finally {
            TransactionSynchronizationManager.unbindResourceIfPossible(entityManagerFactory);
            entities.close();
        }
    }

    @Test
    void disableLockFirstRejectsInFlightHttpAuthenticationAtFinalRegistration() throws Exception {
        delayedAuthentication(false);
    }

    @Test
    void preDisableHttpAuthenticationCannotRegisterAfterDisableThenEnable() throws Exception {
        delayedAuthentication(true);
    }

    private void delayedAuthentication(boolean reenable) throws Exception {
        var verified = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        doAnswer(call -> {
            Authentication result = (Authentication) call.callRealMethod();
            verified.countDown();
            assertTrue(resume.await(30, TimeUnit.SECONDS), "Registration must be released");
            return result;
        }).when(provider).authenticate(any());
        try (Browser delayed = new Browser(); Browser fresh = new Browser(); var executor = Executors.newSingleThreadExecutor()) {
            var login = executor.submit(() -> { delayed.login("synthetic.target", targetPassword, false); return null; });
            try {
                assertTrue(verified.await(30, TimeUnit.SECONDS), "Authentication must finish before lifecycle transition");
                assertTrue(tracked(target).isEmpty());
                service.disable(target); // Obtains and releases the real account lock before registration.
                if (reenable) { service.enable(target); }
                resume.countDown();
                login.get(30, TimeUnit.SECONDS);
                assertTrue(tracked(target).isEmpty());
                assertEvents(1, reenable ? 1 : 0);
                reset(provider);
                if (reenable) { fresh.login("synthetic.target", targetPassword, true); }
            } finally { resume.countDown(); }
        }
    }

    @Test
    void registrationFirstIsExpiredEvenBeforeHttpContextPersistence() throws Exception {
        var registered = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        doAnswer(call -> {
            call.callRealMethod();
            registered.countDown();
            assertTrue(resume.await(30, TimeUnit.SECONDS), "Context persistence must be released");
            return null;
        }).when(registration).register(any(), any());
        try (Browser delayed = new Browser(); var executor = Executors.newSingleThreadExecutor()) {
            var login = executor.submit(() -> { delayed.login("synthetic.target", targetPassword, true, false); return null; });
            try {
                assertTrue(registered.await(30, TimeUnit.SECONDS), "Registration must finish first");
                var session = tracked(target).getFirst();
                service.disable(target);
                assertTrue(session.isExpired());
                resume.countDown();
                login.get(30, TimeUnit.SECONDS);
                assertEquals(302, delayed.get("/test/session").statusCode());
                assertEquals(302, delayed.get("/test/session").statusCode());
                assertTrue(tracked(target).isEmpty());
                assertEvents(1, 0);
            } finally { resume.countDown(); }
        }
    }

    private String snapshot() {
        return jdbc.queryForMap("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,"
                + "locked_until_utc,credential_updated_at_utc,authentication_generation,security_updated_at_utc,row_version "
                + "FROM identity_account WHERE canonical_login='synthetic.target'").toString();
    }
    private String invariant() {
        return jdbc.queryForMap("SELECT id,HEX(public_id),canonical_login,password_hash,credential_updated_at_utc "
                + "FROM identity_account WHERE canonical_login='synthetic.target'").toString();
    }
    private String assignments() {
        return jdbc.queryForList("SELECT id,HEX(public_id),canonical_name,enabled,row_version FROM identity_role ORDER BY id").toString()
                + jdbc.queryForList("SELECT * FROM identity_permission ORDER BY id")
                + jdbc.queryForList("SELECT * FROM identity_account_role ORDER BY account_id,role_id")
                + jdbc.queryForList("SELECT * FROM identity_role_permission ORDER BY role_id,permission_id");
    }
    private String administratorSnapshot() {
        return jdbc.queryForMap("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,"
                + "locked_until_utc,credential_updated_at_utc,authentication_generation,security_updated_at_utc,row_version "
                + "FROM identity_account WHERE canonical_login='synthetic.self'").toString();
    }
    private long generation() {
        return jdbc.queryForObject("SELECT authentication_generation FROM identity_account WHERE canonical_login='synthetic.target'", Long.class);
    }
    private int enabled() {
        return jdbc.queryForObject("SELECT enabled FROM identity_account WHERE canonical_login='synthetic.target'", Integer.class);
    }
    private SessionInformation trackTarget() {
        String session = UUID.randomUUID().toString();
        registry.registerNewSession(session, new AccountPrincipal(target, "synthetic.target"));
        return registry.getSessionInformation(session);
    }
    private List<SessionInformation> tracked(UUID publicId) {
        return registry.getAllPrincipals().stream()
                .filter(value -> value instanceof AccountPrincipal account && publicId.equals(account.publicId()))
                .flatMap(value -> registry.getAllSessions(value, true).stream()).toList();
    }
    private void assertEvents(int disabled, int enabled) {
        assertEquals(disabled, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_ACCOUNT_DISABLED'", Integer.class));
        assertEquals(enabled, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_ACCOUNT_ENABLED'", Integer.class));
        var events = jdbc.queryForList("SELECT actor_reference,target_type,target_reference,reason,context FROM audit_event "
                + "WHERE action IN ('IDENTITY_ACCOUNT_DISABLED','IDENTITY_ACCOUNT_ENABLED')");
        for (var event : events) {
            assertEquals(id.toString(), event.get("actor_reference"));
            assertEquals(target.toString(), event.get("target_reference"));
            assertEquals("IDENTITY_ACCOUNT", event.get("target_type"));
            assertEquals("administrative-account-lifecycle", event.get("context"));
            assertNull(event.get("reason"));
            assertFalse(event.toString().contains(targetPassword));
        }
        flyway.validate();
    }
    private static void bounded(AccountLifecycleException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(AccountLifecycleException.class, action);
        assertEquals(reason, failure.reason());
        assertEquals("Account lifecycle failed: " + reason, failure.getMessage());
        assertNull(failure.getCause());
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
        @Bean @Primary Clock lifecycleTestClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean ServletRegistrationBean<HttpServlet> lifecycleProbe(
                AccountLifecycleService service, CurrentActor actor) {
            return new ServletRegistrationBean<>(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws java.io.IOException {
                    actor.requireUserId();
                    Authentication context = SecurityContextHolder.getContext().getAuthentication();
                    if (context.getCredentials() != null || context.getDetails() != null) {
                        response.setStatus(500); return;
                    }
                    if (request.getPathInfo().equals("/csrf")) {
                        response.getWriter().write(((CsrfToken)request.getAttribute(CsrfToken.class.getName())).getToken());
                    } else {
                        if (request.getSession().getMaxInactiveInterval() != 1800) { response.setStatus(500); return; }
                        response.getWriter().write("authenticated");
                    }
                }
                @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                    UUID target = UUID.fromString(request.getParameter("target"));
                    if (request.getPathInfo().equals("/disable")) { service.disable(target); }
                    else if (request.getPathInfo().equals("/enable")) { service.enable(target); }
                    else { response.setStatus(400); return; }
                    response.setStatus(204);
                }
            }, "/test/*");
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
            login(login, password, success, true);
        }
        void login(String login, String password, boolean success, boolean probeSession) throws Exception {
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
                if (probeSession) { assertEquals(200, get("/test/session").statusCode()); }
            } else {
                assertEquals(302, get("/test/session").statusCode(),
                        "Rejected authentication must not establish a usable authenticated session");
                assertTrue(response.headers().firstValue("location").orElse("").contains("error"));
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

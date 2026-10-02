package io.github.vncntz.hris;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vaadin.productionMode=true", "logging.level.root=OFF"})
@Import({LocalProvisioningIdentityConfiguration.class, RoleAdministrationIT.ProbeConfiguration.class})
class RoleAdministrationIT {
    @Container @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    private static final Instant NOW = Instant.parse("2026-10-02T04:05:06.123456Z");
    @Autowired private AccountRoleAssignmentService membership;
    @Autowired private RoleAdministrationService service;
    @Autowired private FirstAdministratorProvisioner bootstrap;
    @Autowired private AccountCreationService creation;
    @MockitoSpyBean private AccountAuthenticationProvider provider;
    @Autowired private CurrentActor actor;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Flyway flyway;
    @MockitoSpyBean private SessionRegistry registry;

    @MockitoSpyBean private AccountSessionRegistrationService registration;
    @MockitoSpyBean private AuditRecorder audit;
    @MockitoSpyBean private AuthenticatedSessionRevoker revoker;
    @Value("${local.server.port}") private int port;
    private UUID id;
    private Authentication authenticated;
    private String oldPassword;
    private UUID role;
    private UUID target;
    private UUID changedRole;
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
        role = UUID.fromString(jdbc.queryForObject("SELECT BIN_TO_UUID(public_id) FROM identity_role WHERE canonical_name='administrator'", String.class));
        changedRole = service.createRole("synthetic.configured");
        service.createPermission("test:read");
        clearInvocations(audit, revoker);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }


    @Test void completeHttpLifecycleInvalidatesAllAssignedAccountsAndPreservesUnrelatedSessions() throws Exception {
        assertEquals(0, jdbc.queryForObject("SELECT enabled FROM identity_role WHERE public_id=UUID_TO_BIN(?)", Integer.class, changedRole.toString()));
        assertEquals(0, roleGeneration());
        service.assignPermission(changedRole, "test:read");
        assertEquals(1, roleGeneration());
        service.enable(changedRole);
        membership.assign(target, changedRole);
        String password2 = UUID.randomUUID().toString();
        UUID target2 = creation.create("synthetic.second", password2.toCharArray(), password2.toCharArray()).publicId();
        membership.assign(target2, changedRole);
        try (Browser first = new Browser(); Browser second = new Browser(); Browser third = new Browser();
                Browser admin = new Browser(); Browser fresh = new Browser()) {
            first.login("synthetic.target", targetPassword, true);
            second.login("synthetic.target", targetPassword, true);
            third.login("synthetic.second", password2, true);
            admin.login("synthetic.self", oldPassword, true);
            assertEquals("true", first.get("/test/session").body());
            var assigned = new java.util.ArrayList<>(tracked(target));
            assigned.addAll(tracked(target2));
            assertEquals(3, assigned.size());
            var unrelated = tracked(id);
            String csrf = admin.get("/test/csrf").body();
            String accountState = accountsState();
            assertEquals(403, admin.post("/test/remove?role=" + changedRole, null).statusCode());
            assertEquals(204, admin.post("/test/remove?role=" + changedRole, csrf).statusCode());
            assertEquals(3, roleGeneration());
            for (var session : assigned) { assertTrue(session.isExpired()); }
            assertTrue(accountState.equals(accountsState()));
            for (Browser browser : List.of(first, second, third)) { assertEquals(302, browser.get("/test/session").statusCode()); }
            fresh.login("synthetic.target", targetPassword, true);
            assertEquals("false", fresh.get("/test/session").body());
            service.assignPermission(changedRole, "test:read");
            assertEquals(302, fresh.get("/test/session").statusCode());
            first.login("synthetic.target", targetPassword, true);
            third.login("synthetic.second", password2, true);
            assertEquals("true", first.get("/test/session").body());
            var enabled = new java.util.ArrayList<>(tracked(target)); enabled.addAll(tracked(target2));
            service.disable(changedRole);
            assertEquals(5, roleGeneration());
            for (var session : enabled) { assertTrue(session.isExpired()); }
            second.login("synthetic.target", targetPassword, true);
            assertEquals("false", second.get("/test/session").body());
            for (var session : unrelated) { assertFalse(session.isExpired()); }
            assertEquals(200, admin.get("/test/session").statusCode());
        }
        var events = jdbc.queryForList("SELECT actor_reference,action,target_type,target_reference,reason,context FROM audit_event "
                + "WHERE action LIKE 'IDENTITY_ROLE_%' OR action='IDENTITY_PERMISSION_CREATED'");
        assertEquals(7, events.size());
        for (var event : events) {
            assertEquals(id.toString(), event.get("actor_reference"));
            assertNull(event.get("reason"));
            String action = event.get("action").toString();
            assertEquals(action.equals("IDENTITY_PERMISSION_CREATED") ? "test:read" : changedRole.toString(), event.get("target_reference"));
            assertEquals(action.equals("IDENTITY_PERMISSION_CREATED") ? "IDENTITY_PERMISSION"
                    : action.contains("ROLE_PERMISSION") ? "IDENTITY_ROLE_PERMISSION" : "IDENTITY_ROLE", event.get("target_type"));
            assertEquals(action.equals("IDENTITY_PERMISSION_CREATED") ? "administrative-permission-management"
                    : action.contains("ROLE_PERMISSION") ? "administrative-role-permission-assignment/test:read"
                    : "administrative-role-management", event.get("context"));
        }
    }

    @Test void selfAdminDisableAndPermissionRemovalRejectPersistedLastAuthority() {
        bounded(RoleAdministrationException.Reason.SELF_ADMIN_REMOVAL_REJECTED, () -> service.disable(role));
        bounded(RoleAdministrationException.Reason.SELF_ADMIN_REMOVAL_REJECTED, () -> service.removePermission(role, "identity:admin"));
        verifyNoInteractions(audit, revoker);
        assertEquals(0L, jdbc.queryForObject("SELECT authorization_generation FROM identity_role WHERE public_id=UUID_TO_BIN(?)", Long.class, role.toString()));
        UUID alternate = service.createRole("synthetic.alternate");
        service.assignPermission(alternate, "identity:admin");
        service.enable(alternate);
        membership.assign(id, alternate);
        service.removePermission(role, "identity:admin");
        service.disable(role);
    }

    @Test void maximumLengthPermissionIsStoredAndAuditedIntact() {
        String key = "a".repeat(128);
        assertEquals(key, service.createPermission(key));
        service.assignPermission(changedRole, key);
        service.removePermission(changedRole, key);
        var contexts = jdbc.queryForList("SELECT target_reference,context FROM audit_event WHERE target_type='IDENTITY_ROLE_PERMISSION'");
        assertEquals(2, contexts.size());
        for (var event : contexts) {
            assertEquals(changedRole.toString(), event.get("target_reference"));
            assertEquals("administrative-role-permission-assignment/" + key, event.get("context"));
        }
    }

    @Test void auditFailureRollsBackWithoutExpiry() {
        configure();
        String before = roleState();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("Synthetic audit failure"); }).when(audit).record(any());
        bounded(RoleAdministrationException.Reason.PERSISTENCE_FAILED, () -> service.removePermission(changedRole, "test:read"));
        assertTrue(before.equals(roleState()));
        verifyNoInteractions(revoker);
    }

    @Test void flushFailureRollsBackWithoutExpiry() {
        configure();
        String before = roleState();
        jdbc.execute("CREATE TRIGGER synthetic_role_failure BEFORE UPDATE ON identity_role FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic role mutation failure'");
        try {
            bounded(RoleAdministrationException.Reason.PERSISTENCE_FAILED, () -> service.disable(changedRole));
            assertTrue(before.equals(roleState()));
            verifyNoInteractions(revoker);
        } finally { jdbc.execute("DROP TRIGGER synthetic_role_failure"); }
    }

    @Test void revocationFailureRollsBackFlushedRoleGenerationMembershipAndAudit() throws Exception {
        failureAfterExpiry(false);
    }

    @Test void lateCommitFailureRetainsOldAuthoritiesWithExpiredSessions() throws Exception {
        failureAfterExpiry(true);
    }

    private void failureAfterExpiry(boolean commitFailure) throws Exception {
        configure();
        try (Browser browser = new Browser()) {
            browser.login("synthetic.target", targetPassword, true);
            var session = tracked(target).getFirst();
            String before = roleState();
            doAnswer(call -> {
                assertEquals(3, roleGeneration());
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id "
                        + "WHERE r.public_id=UUID_TO_BIN(?)", Integer.class, changedRole.toString()));
                call.callRealMethod();
                if (!commitFailure) { throw new IllegalStateException("Synthetic revocation failure"); }
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) { throw new IllegalStateException("Synthetic late commit failure"); }
                });
                return null;
            }).when(revoker).revoke(java.util.Set.of(target));
            bounded(commitFailure ? RoleAdministrationException.Reason.PERSISTENCE_FAILED
                    : RoleAdministrationException.Reason.SESSION_REVOCATION_FAILED,
                    () -> service.removePermission(changedRole, "test:read"));
            assertTrue(before.equals(roleState()));
            assertTrue(session.isExpired());
            assertEquals(302, browser.get("/test/session").statusCode());
            browser.login("synthetic.target", targetPassword, true);
            assertEquals("true", browser.get("/test/session").body());
        }
    }

    @Test void disabledAssignedRoleEnableRejectsPreMutationHttpAuthentication() throws Exception { mutationFirst(true); }
    @Test void permissionRemovalRejectsPreMutationHttpAuthentication() throws Exception { mutationFirst(false); }

    private void mutationFirst(boolean enable) throws Exception {
        configure();
        if (enable) { service.disable(changedRole); }
        var verified = new CountDownLatch(1); var resume = new CountDownLatch(1);
        doAnswer(call -> {
            Authentication result = (Authentication)call.callRealMethod();
            verified.countDown(); assertTrue(resume.await(30, TimeUnit.SECONDS)); return result;
        }).when(provider).authenticate(any());
        try (Browser delayed = new Browser(); var executor = Executors.newSingleThreadExecutor()) {
            var login = executor.submit(() -> { delayed.login("synthetic.target", targetPassword, false); return null; });
            try {
                assertTrue(verified.await(30, TimeUnit.SECONDS));
                assertTrue(tracked(target).isEmpty());
                if (enable) { service.enable(changedRole); } else { service.removePermission(changedRole, "test:read"); }
                resume.countDown(); login.get(30, TimeUnit.SECONDS);
                assertTrue(tracked(target).isEmpty());
                reset(provider);
                delayed.login("synthetic.target", targetPassword, true);
                assertEquals(Boolean.toString(enable), delayed.get("/test/session").body());
            } finally { resume.countDown(); }
        }
    }

    @Test void registrationFirstIsObservedAndExpiredBeforeContextPersistence() throws Exception {
        configure();
        var registered = new CountDownLatch(1); var resume = new CountDownLatch(1);
        doAnswer(call -> {
            call.callRealMethod(); registered.countDown(); assertTrue(resume.await(30, TimeUnit.SECONDS)); return null;
        }).when(registration).register(any(), any());
        try (Browser delayed = new Browser(); var executor = Executors.newSingleThreadExecutor()) {
            var login = executor.submit(() -> { delayed.login("synthetic.target", targetPassword, true, false); return null; });
            try {
                assertTrue(registered.await(30, TimeUnit.SECONDS));
                var session = tracked(target).getFirst();
                service.removePermission(changedRole, "test:read");
                assertTrue(session.isExpired());
                resume.countDown(); login.get(30, TimeUnit.SECONDS);
                assertEquals(302, delayed.get("/test/session").statusCode());
                assertTrue(tracked(target).isEmpty());
            } finally { resume.countDown(); }
        }
    }

    @Test void accountMembershipFirstSerializesWithRoleDisable() throws Exception { membershipRace(true); }
    @Test void roleDisableFirstSerializesAndRejectsAccountAssignment() throws Exception { membershipRace(false); }

    private void membershipRace(boolean membershipFirst) throws Exception {
        service.assignPermission(changedRole, "test:read"); service.enable(changedRole);
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1); var started = new CountDownLatch(1);
        if (membershipFirst) {
            doAnswer(call -> { locked.countDown(); assertTrue(release.await(30, TimeUnit.SECONDS)); return call.callRealMethod(); }).when(revoker).revoke(target);
        } else {
            doAnswer(call -> { locked.countDown(); assertTrue(release.await(30, TimeUnit.SECONDS)); return call.callRealMethod(); }).when(revoker).revoke(java.util.Set.<UUID>of());
        }
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { withActor(() -> {
                if (membershipFirst) { membership.assign(target, changedRole); } else { service.disable(changedRole); }
            }); return null; });
            try {
                assertTrue(locked.await(30, TimeUnit.SECONDS));
                var second = executor.submit(() -> { started.countDown(); withActor(() -> {
                    if (membershipFirst) { service.disable(changedRole); }
                    else {
                        var failure = assertThrows(AccountRoleAssignmentException.class, () -> membership.assign(target, changedRole));
                        assertEquals(AccountRoleAssignmentException.Reason.ROLE_DISABLED, failure.reason());
                    }
                }); return null; });
                assertTrue(started.await(30, TimeUnit.SECONDS));
                release.countDown(); first.get(30, TimeUnit.SECONDS); second.get(30, TimeUnit.SECONDS);
                assertEquals(3, roleGeneration());
                assertEquals(membershipFirst ? 1 : 0, jdbc.queryForObject("SELECT COUNT(*) FROM identity_account_role ar "
                        + "JOIN identity_account a ON a.id=ar.account_id JOIN identity_role r ON r.id=ar.role_id "
                        + "WHERE a.public_id=UUID_TO_BIN(?) AND r.public_id=UUID_TO_BIN(?)", Integer.class,
                        target.toString(), changedRole.toString()));
                try (Browser fresh = new Browser()) {
                    fresh.login("synthetic.target", targetPassword, true);
                    assertEquals("false", fresh.get("/test/session").body());
                }
            } finally { release.countDown(); }
        }
    }

    private void configure() {
        service.assignPermission(changedRole, "test:read"); service.enable(changedRole);
        membership.assign(target, changedRole); clearInvocations(audit, revoker);
    }
    private long roleGeneration() {
        return jdbc.queryForObject("SELECT authorization_generation FROM identity_role WHERE public_id=UUID_TO_BIN(?)", Long.class, changedRole.toString());
    }
    private String roleState() {
        return jdbc.queryForList("SELECT id,HEX(public_id),canonical_name,enabled,authorization_generation,row_version FROM identity_role ORDER BY id").toString()
                + jdbc.queryForList("SELECT * FROM identity_role_permission ORDER BY role_id,permission_id")
                + jdbc.queryForList("SELECT HEX(event_id),action,target_reference FROM audit_event ORDER BY id");
    }
    private String accountsState() {
        return jdbc.queryForList("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,authentication_generation,"
                + "credential_updated_at_utc,security_updated_at_utc,failed_attempts,locked_until_utc,row_version FROM identity_account ORDER BY id").toString()
                + jdbc.queryForList("SELECT * FROM identity_account_role ORDER BY account_id,role_id");
    }
    private void withActor(Runnable command) {
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        try { command.run(); } finally { SecurityContextHolder.clearContext(); }
    }
    private static void bounded(RoleAdministrationException.Reason reason, Runnable command) {
        var failure = assertThrows(RoleAdministrationException.class, command::run);
        assertEquals(reason, failure.reason()); assertNull(failure.getCause());
    }

    private List<SessionInformation> tracked(UUID publicId) {
        return registry.getAllPrincipals().stream()
                .filter(value -> value instanceof AccountPrincipal account && publicId.equals(account.publicId()))
                .flatMap(value -> registry.getAllSessions(value, true).stream()).toList();
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
        @Bean @Primary Clock roleTestClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean ServletRegistrationBean<HttpServlet> roleProbe(
                RoleAdministrationService service, CurrentActor actor) {
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
                        response.getWriter().write(Boolean.toString(actor.hasAuthority("test:read")));
                    }
                }
                @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                    UUID role = UUID.fromString(request.getParameter("role"));
                    if (request.getPathInfo().equals("/enable")) { service.enable(role); }
                    else if (request.getPathInfo().equals("/disable")) { service.disable(role); }
                    else if (request.getPathInfo().equals("/assign")) { service.assignPermission(role, "test:read"); }
                    else if (request.getPathInfo().equals("/remove")) { service.removePermission(role, "test:read"); }
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

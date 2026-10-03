package io.github.vncntz.hris;

import java.net.*;
import java.net.http.*;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import io.github.vncntz.hris.identityaccess.*;
import jakarta.servlet.http.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vaadin.productionMode=true", "logging.level.root=OFF", "hris.security.reauthentication-window=PT2M"})
@Import({LocalProvisioningIdentityConfiguration.class, CredentialReauthenticationIT.ProbeConfiguration.class})
class CredentialReauthenticationIT {
    @Container @ServiceConnection static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    static final Instant NOW = Instant.parse("2026-10-03T04:00:00Z");
    @Autowired FirstAdministratorProvisioner bootstrap;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired SessionRegistry registry;
    @Autowired AuthenticatedSessionRevoker revoker;
    @Autowired TestClock clock;
    @MockitoSpyBean PasswordEncoder encoder;
    @Value("${local.server.port}") int port;
    UUID id;
    String credential;

    @BeforeEach void fresh() {
        registry.getAllPrincipals().forEach(principal -> registry.getAllSessions(principal, true)
                .forEach(session -> registry.removeSessionInformation(session.getSessionId())));
        Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).cleanDisabled(false).load().clean();
        flyway.migrate(); clock.now = NOW;
        credential = UUID.randomUUID().toString();
        id = bootstrap.provision("synthetic.admin", credential.toCharArray(), credential.toCharArray());
    }

    @Test void authorityAloneCannotMutateAndProofIsIsolatedToOneRealSession() throws Exception {
        try (Browser first = new Browser(); Browser second = new Browser()) {
            first.login("synthetic.admin", credential); second.login("synthetic.admin", credential);
            assertEquals(428, first.post("role", "").statusCode());
            assertEquals(403, first.post("proof", "credential=" + encode(UUID.randomUUID().toString())).statusCode());
            assertEquals(428, first.post("role", "").statusCode());
            long audits = count("audit_event"); String before = accountState();
            assertEquals(204, first.prove(credential).statusCode());
            assertTrue(before.equals(accountState()), "Proof must not mutate Account security state");
            assertEquals(audits, count("audit_event"));
            assertEquals(204, first.post("role", "").statusCode());
            assertEquals(428, second.post("permission", "").statusCode());
            assertEquals(204, second.prove(credential).statusCode());
            assertEquals(204, second.post("permission", "").statusCode());
        }
    }
    @Test void allExistingAdministrativeCommandsDenyMissingProofBeforeValidationOrMutation() throws Exception {
        try (Browser browser = new Browser()) {
            browser.login("synthetic.admin", credential);
            for (String command : java.util.List.of("create", "enableAccount", "disableAccount", "assignRole", "removeRole",
                    "role", "enableRole", "disableRole", "permission", "assignPermission", "removePermission")) {
                assertEquals(428, browser.post(command, "").statusCode());
            }
            assertEquals(1, count("identity_account")); assertEquals(1, count("audit_event"));
        }
    }
    @Test void exactConfiguredExpiryAndRollbackFailClosedAndSuccessfulProofReplacesMarker() throws Exception {
        try (Browser browser = new Browser()) {
            browser.login("synthetic.admin", credential); assertEquals(204, browser.prove(credential).statusCode());
            clock.now = NOW.plusSeconds(119); assertEquals(204, browser.post("role", "").statusCode());
            clock.now = NOW.plusSeconds(120); assertEquals(428, browser.post("permission", "").statusCode());
            assertEquals(204, browser.prove(credential).statusCode());
            clock.now = NOW.plusSeconds(119); assertEquals(428, browser.post("permission", "").statusCode());
            clock.now = NOW.plusSeconds(121); assertEquals(204, browser.post("permission", "").statusCode());
        }
    }
    @Test void failedExplicitAttemptDiscardsPreviouslyValidProofAndCsrfStillProtectsProof() throws Exception {
        try (Browser browser = new Browser()) {
            browser.login("synthetic.admin", credential); assertEquals(204, browser.prove(credential).statusCode());
            assertEquals(403, browser.send("proof", "credential=" + encode(credential), false).statusCode());
            assertEquals(403, browser.prove(UUID.randomUUID().toString()).statusCode());
            assertEquals(428, browser.post("role", "").statusCode());
        }
    }
    @Test void logoutAndFreshLoginBeginWithoutProofAndRevocationMakesEarlierProofUnusable() throws Exception {
        try (Browser browser = new Browser()) {
            browser.login("synthetic.admin", credential); assertEquals(204, browser.prove(credential).statusCode());
            assertEquals(302, browser.sendAbsolute("/logout", "", true).statusCode());
            browser.login("synthetic.admin", credential); assertEquals(428, browser.post("role", "").statusCode());
            assertEquals(204, browser.prove(credential).statusCode()); revoker.revoke(id);
            assertEquals(302, browser.post("role", "").statusCode());
            browser.login("synthetic.admin", credential); assertEquals(428, browser.post("role", "").statusCode());
        }
    }
    @Test void proofDoesNotGrantAdministrativeAuthority() throws Exception {
        String ordinaryCredential = UUID.randomUUID().toString();
        try (Browser admin = new Browser(); Browser ordinary = new Browser()) {
            admin.login("synthetic.admin", credential); assertEquals(204, admin.prove(credential).statusCode());
            assertEquals(204, admin.post("create", "credential=" + encode(ordinaryCredential)).statusCode());
            ordinary.login("synthetic.ordinary", ordinaryCredential);
            assertEquals(403, ordinary.post("role", "").statusCode());
            assertEquals(204, ordinary.prove(ordinaryCredential).statusCode());
            assertEquals(403, ordinary.post("role", "").statusCode());
        }
    }
    @Test void concurrentPasswordReplacementRejectsVerifiedOldCredentialWithoutUsableProof() throws Exception {
        CountDownLatch verified = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean pause = new AtomicBoolean(true);
        doAnswer(call -> {
            boolean matched = (boolean) call.callRealMethod();
            if (call.getArgument(0) instanceof CharBuffer && pause.compareAndSet(true, false)) {
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                verified.countDown(); await(release);
            }
            return matched;
        }).when(encoder).matches(any(), anyString());
        String replacement = UUID.randomUUID().toString();
        try (Browser proof = new Browser(); Browser change = new Browser(); var executor = Executors.newSingleThreadExecutor()) {
            proof.login("synthetic.admin", credential); change.login("synthetic.admin", credential);
            var result = executor.submit(() -> proof.prove(credential));
            try {
                assertTrue(verified.await(30, TimeUnit.SECONDS));
                assertEquals(204, change.post("password", "current=" + encode(credential) + "&replacement=" + encode(replacement)).statusCode());
            } finally { release.countDown(); }
            assertEquals(403, result.get(30, TimeUnit.SECONDS).statusCode());
            assertEquals(302, proof.post("role", "").statusCode());
            proof.login("synthetic.admin", replacement); assertEquals(428, proof.post("role", "").statusCode());
        } finally { release.countDown(); }
    }
    @Test void passwordChangeRevokesProofThatSucceededBeforeReplacement() throws Exception {
        try (Browser proof = new Browser(); Browser change = new Browser()) {
            proof.login("synthetic.admin", credential); change.login("synthetic.admin", credential);
            assertEquals(204, proof.prove(credential).statusCode());
            String replacement = UUID.randomUUID().toString();
            assertEquals(204, change.post("password", "current=" + encode(credential) + "&replacement=" + encode(replacement)).statusCode());
            assertEquals(302, proof.post("role", "").statusCode());
            proof.login("synthetic.admin", replacement); assertEquals(428, proof.post("role", "").statusCode());
        }
    }
    long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    String accountState() { return jdbc.queryForList("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,authentication_generation,credential_updated_at_utc,security_updated_at_utc,failed_attempts,locked_until_utc,row_version FROM identity_account").toString(); }
    static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    static void await(CountDownLatch latch) {
        try { if (!latch.await(30, TimeUnit.SECONDS)) { throw new IllegalStateException("Test barrier timed out"); } }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException("Test interrupted"); }
    }

    @TestConfiguration(proxyBeanMethods = false) static class ProbeConfiguration {
        @Bean @Primary TestClock proofClock() { return new TestClock(); }
        @Bean static BeanPostProcessor probeRouteAuthorization() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof HttpSecurity http) {
                        try { http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                request -> request.getRequestURI().startsWith("/test/")).authenticated()); }
                        catch (Exception failure) { throw new IllegalStateException("Test route configuration failed"); }
                    }
                    return bean;
                }
            };
        }
        @Bean ServletRegistrationBean<HttpServlet> proofProbe(CredentialReauthenticationService proof,
                AccountCreationService creation, AccountLifecycleService lifecycle, AccountRoleAssignmentService membership,
                RoleAdministrationService roles, PasswordChangeService passwords, CurrentActor actor) {
            return new ServletRegistrationBean<>(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
                    actor.requireUserId();
                    response.getWriter().write(((CsrfToken) request.getAttribute(CsrfToken.class.getName())).getToken());
                }
                @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                    try {
                        UUID target = UUID.randomUUID();
                        switch (request.getPathInfo()) {
                            case "/proof" -> proof.reauthenticate(request.getParameter("credential").toCharArray());
                            case "/password" -> passwords.change(request.getParameter("current").toCharArray(),
                                    request.getParameter("replacement").toCharArray(), request.getParameter("replacement").toCharArray());
                            case "/role" -> roles.createRole("synthetic." + target);
                            case "/permission" -> roles.createPermission("test:" + target);
                            case "/enableRole" -> roles.enable(target);
                            case "/disableRole" -> roles.disable(target);
                            case "/assignPermission" -> roles.assignPermission(target, "test:read");
                            case "/removePermission" -> roles.removePermission(target, "test:read");
                            case "/enableAccount" -> lifecycle.enable(target);
                            case "/disableAccount" -> lifecycle.disable(target);
                            case "/assignRole" -> membership.assign(target, target);
                            case "/removeRole" -> membership.remove(target, target);
                            case "/create" -> creation.create("synthetic.ordinary", request.getParameter("credential") == null ? null
                                    : request.getParameter("credential").toCharArray(), request.getParameter("credential") == null ? null
                                    : request.getParameter("credential").toCharArray());
                            default -> { response.setStatus(400); return; }
                        }
                        response.setStatus(204);
                    } catch (RecentAuthenticationException failure) {
                        response.setStatus(failure.reason() == RecentAuthenticationException.Reason.PROOF_REQUIRED ? 428 : 403);
                    } catch (org.springframework.security.access.AccessDeniedException failure) { response.setStatus(403); }
                }
            }, "/test/*");
        }
    }
    static final class TestClock extends Clock {
        volatile Instant now = NOW;
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    }
    final class Browser implements AutoCloseable {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> prove(String password) throws Exception { return post("proof", "credential=" + encode(password)); }
        HttpResponse<String> post(String action, String form) throws Exception { return send(action, form, true); }
        HttpResponse<String> send(String action, String form, boolean csrf) throws Exception { return sendAbsolute("/test/" + action, form, csrf); }
        HttpResponse<String> sendAbsolute(String path, String form, boolean csrf) throws Exception {
            var request = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/x-www-form-urlencoded");
            if (csrf) { request.header("X-CSRF-TOKEN", get("/test/csrf").body()); }
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        }
        void login(String login, String password) throws Exception {
            assertEquals(200, get("/login").statusCode());
            var response = client.send(HttpRequest.newBuilder(uri("/login")).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("username=" + encode(login) + "&password=" + encode(password))).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(302, response.statusCode()); assertEquals(200, get("/test/csrf").statusCode());
        }
        @Override public void close() { client.close(); }
    }
}

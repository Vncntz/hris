package io.github.vncntz.hris;

import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import io.github.vncntz.hris.identityaccess.*;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
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
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vaadin.productionMode=true", "logging.level.root=OFF"})
@Import({LocalProvisioningIdentityConfiguration.class, AdministrativeTestSessionConfiguration.class, OfflineMfaIT.Probes.class})
class OfflineMfaIT {
    @Container @ServiceConnection static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    static final Instant NOW = Instant.parse("2026-10-03T04:00:00Z");
    static final Path KEY = key();
    static Path key() {
        try {
            Path path = Files.createTempFile("synthetic-mfa-", ".key");
            byte[] bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes);
            Files.write(path, bytes); Arrays.fill(bytes, (byte) 0); path.toFile().deleteOnExit(); return path;
        } catch (Exception failure) { throw new IllegalStateException("Synthetic key setup failed"); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry values) {
        values.add("hris.security.mfa-key-file", () -> KEY.toString());
    }
    @Autowired MfaAdministrationService mfa;
    @Autowired FirstAdministratorProvisioner bootstrap;
    @MockitoSpyBean AccountAuthenticationProvider provider;
    @Autowired CredentialReauthenticationService proof;
    @Autowired RecentAuthenticationSession recent;
    @Autowired AccountCreationService creation;
    @Autowired AccountRoleAssignmentService membership;
    @Autowired PasswordResetService passwords;
    @Autowired AccountSessionRegistrationService registration;
    @Autowired SessionRegistry sessions;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired TestClock clock;
    @MockitoSpyBean AuditRecorder audit;
    @MockitoSpyBean AuthenticatedSessionRevoker revoker;
    @Value("${local.server.port}") int port;
    UUID id;
    String password;
    Authentication before;
    byte[] seed;
    char[][] recovery;
    @BeforeEach void fresh() {
        clock.now = NOW;
        for (var principal : sessions.getAllPrincipals()) {
            for (var session : sessions.getAllSessions(principal, true)) { sessions.removeSessionInformation(session.getSessionId()); }
        }
        Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).cleanDisabled(false).load().clean();
        flyway.migrate(); password = UUID.randomUUID().toString();
        id = bootstrap.provision("synthetic.admin", password.toCharArray(), password.toCharArray());
        before = authenticate(null); SecurityContextHolder.getContext().setAuthentication(before);
        proof.reauthenticate(password.toCharArray()); clearInvocations(audit, revoker);
    }
    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
        if (seed != null) { Arrays.fill(seed, (byte) 0); }
        if (recovery != null) { for (var value : recovery) { Arrays.fill(value, '\0'); } }
    }
    void begin() {
        try (var material = mfa.beginEnrollment()) {
            char[] value = material.values()[0]; seed = decode(value);
            assertFalse(Arrays.equals(seed, jdbc.queryForObject("SELECT mfa_secret FROM identity_account WHERE canonical_login='synthetic.admin'", byte[].class)));
        }
    }
    void enroll() {
        begin(); char[] code = code(0);
        try (var material = mfa.confirmEnrollment(code)) {
            recovery = Arrays.stream(material.values()).map(char[]::clone).toArray(char[][]::new);
        }
        cleared(code);
    }
    char[] code(int offset) { return otp(seed, clock.now.getEpochSecond() / 30 + offset); }
    Authentication authenticate(char[] factor) {
        var token = UsernamePasswordAuthenticationToken.unauthenticated("synthetic.admin", password);
        if (factor != null) { token.setDetails(new MfaInput(factor)); }
        return provider.authenticate(token);
    }
    int failures() { return jdbc.queryForObject("SELECT failed_attempts FROM identity_account WHERE canonical_login='synthetic.admin'", Integer.class); }
    long generation() { return jdbc.queryForObject("SELECT authentication_generation FROM identity_account WHERE canonical_login='synthetic.admin'", Long.class); }
    void next() { clock.now = clock.now.plusSeconds(30); }
    void fullProof() { next(); proof.reauthenticate(password.toCharArray(), code(0)); }

    @Test void enrollmentConfirmsPossessionRevokesAndRejectsStaleRegistration() {
        sessions.registerNewSession(UUID.randomUUID().toString(), before.getPrincipal());
        begin(); assertEquals(0, generation());
        Authentication pending = authenticate(null); assertTrue(pending.isAuthenticated());
        char[] wrong = new char[6]; assertThrows(MfaException.class, () -> mfa.confirmEnrollment(wrong)); cleared(wrong);
        assertEquals(1, failures());
        try (var result = mfa.confirmEnrollment(code(0))) { assertEquals(10, result.values().length); }
        assertEquals(1, generation()); assertEquals(0, failures());
        assertTrue(sessions.getAllSessions(before.getPrincipal(), true).stream().allMatch(SessionInformation::isExpired));
        assertThrows(org.springframework.security.web.authentication.session.SessionAuthenticationException.class,
                () -> registration.register(before, () -> fail("Stale registration must be rejected")));
        assertNull(before.getDetails());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_MFA_ENABLED'", Integer.class));
        assertThrows(BadCredentialsException.class, () -> authenticate(null));
    }
    @Test void pendingExpiryAndConfirmationLockoutFailClosed() {
        begin(); clock.now = NOW.plusSeconds(600);
        proof.reauthenticate(password.toCharArray());
        assertThrows(MfaException.class, () -> mfa.confirmEnrollment(code(0)));
        assertEquals(0, generation());
        clock.now = NOW; proof.reauthenticate(password.toCharArray()); begin();
        for (int i = 0; i < 5; i++) { assertThrows(MfaException.class, () -> mfa.confirmEnrollment(new char[6])); }
        assertEquals(5, failures());
        assertThrows(MfaException.class, () -> mfa.confirmEnrollment(code(0)));
        assertEquals(0, generation());
    }
    @Test void replayPersistsAcrossFreshContextsAndRecoveryCodesAreOneUse() {
        enroll(); next(); char[] factor = code(0);
        Authentication authenticated = authenticate(factor); cleared(factor);
        assertNull(authenticated.getCredentials());
        registration.register(authenticated, () -> { }); assertNull(authenticated.getDetails());
        assertThrows(BadCredentialsException.class, () -> authenticate(code(0)));
        assertThrows(BadCredentialsException.class, () -> authenticate(null));
        char[] recoveryCode = recovery[0].clone(); authenticate(recoveryCode); cleared(recoveryCode);
        assertThrows(BadCredentialsException.class, () -> authenticate(recovery[0].clone()));
        assertEquals(1, failures());
        assertEquals(9, jdbc.queryForObject("SELECT mfa_recovery_hashes FROM identity_account", String.class).split(",").length);
    }
    @Test void concurrentTotpAndRecoveryConsumptionPermitExactlyOneSuccess() throws Exception {
        enroll(); next(); assertEquals(1, concurrent(code(0)));
        assertEquals(1, concurrent(recovery[0]));
        assertEquals(9, jdbc.queryForObject("SELECT mfa_recovery_hashes FROM identity_account", String.class).split(",").length);
    }
    int concurrent(char[] factor) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var calls = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++) { calls.add(pool.submit(() -> {
                ready.countDown(); assertTrue(start.await(20, TimeUnit.SECONDS));
                try { authenticate(factor.clone()); return true; } catch (BadCredentialsException rejected) { return false; }
            })); }
            assertTrue(ready.await(20, TimeUnit.SECONDS)); start.countDown();
            int successes = 0; for (var call : calls) { if (call.get(30, TimeUnit.SECONDS)) { successes++; } } return successes;
        }
    }
    @Test void fullFactorRecentProofIsRequiredAndFailuresCommitExactlyOnce() {
        enroll(); next();
        assertThrows(RecentAuthenticationException.class, () -> proof.reauthenticate(password.toCharArray()));
        assertTrue(recent.proof().isEmpty()); assertEquals(1, failures());
        char[] factor = code(0); proof.reauthenticate(password.toCharArray(), factor); cleared(factor);
        assertTrue(recent.proof().isPresent()); assertEquals(0, failures());
        assertThrows(RecentAuthenticationException.class, () -> proof.reauthenticate(password.toCharArray(), code(0)));
        assertTrue(recent.proof().isEmpty()); assertEquals(1, failures());
        assertThrows(RecentAuthenticationException.class, mfa::disable);
    }
    @Test void recoveryReplacementAndSelfDisablePreserveCredentialAndAuthorities() {
        enroll(); String encoding = jdbc.queryForObject("SELECT password_hash FROM identity_account", String.class);
        fullProof(); long old = generation();
        try (var replacement = mfa.replaceRecoveryCodes()) { assertEquals(10, replacement.values().length); }
        assertEquals(old + 1, generation());
        assertThrows(BadCredentialsException.class, () -> authenticate(recovery[0].clone()));
        fullProof(); mfa.disable(); assertEquals(old + 2, generation());
        assertNull(jdbc.queryForObject("SELECT mfa_secret FROM identity_account", byte[].class));
        assertEquals(encoding, jdbc.queryForObject("SELECT password_hash FROM identity_account", String.class));
        assertTrue(authenticate(null).getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("identity:admin")));
    }
    @Test void authorizationSelfResetAndAmbientTransactionDenialsClearBuffers() {
        char[] factor = new char[6]; SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> mfa.confirmEnrollment(factor)); cleared(factor);
        SecurityContextHolder.getContext().setAuthentication(before);
        assertThrows(MfaException.class, () -> mfa.reset(id));
        new TransactionTemplate(transactions).executeWithoutResult(status -> assertThrows(MfaException.class, mfa::beginEnrollment));
        assertNull(jdbc.queryForObject("SELECT mfa_secret FROM identity_account", byte[].class));
        UUID ordinary = creation.create("synthetic.ordinary", password.toCharArray(), password.toCharArray()).publicId();
        var ordinaryAuth = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.ordinary", password));
        SecurityContextHolder.getContext().setAuthentication(ordinaryAuth); proof.reauthenticate(password.toCharArray());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, mfa::beginEnrollment);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> mfa.reset(id));
        assertNotEquals(ordinary, id);
    }
    @Test void auditAndRevocationFailureRollbackWithoutLeakingCauses() {
        begin();
        doThrow(new IllegalStateException(UUID.randomUUID().toString())).when(audit).record(any());
        var failure = assertThrows(MfaException.class, () -> mfa.confirmEnrollment(code(0))); assertNull(failure.getCause());
        assertEquals(0, generation()); reset(audit);
        doThrow(new IllegalStateException("Synthetic revocation failure")).when(revoker).revoke(id);
        assertThrows(MfaException.class, () -> mfa.confirmEnrollment(code(0)));
        assertEquals(0, generation()); assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_MFA_ENABLED'", Integer.class));
        reset(revoker);
        try (var material = mfa.confirmEnrollment(code(0))) { assertEquals(10, material.values().length); }
    }
    @Test void lateCommitFailureLeavesOnlySafeExpiryAndAllowsFreshConfirmation() {
        begin(); String sessionId = UUID.randomUUID().toString();
        sessions.registerNewSession(sessionId, before.getPrincipal());
        doAnswer(call -> {
            call.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) { throw new IllegalStateException("Synthetic commit failure"); }
            });
            return null;
        }).when(revoker).revoke(id);
        assertThrows(MfaException.class, () -> mfa.confirmEnrollment(code(0)));
        assertTrue(sessions.getSessionInformation(sessionId).isExpired()); assertEquals(0, generation());
        assertFalse(jdbc.queryForObject("SELECT mfa_enabled FROM identity_account", Boolean.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_MFA_ENABLED'", Integer.class));
        reset(revoker);
        try (var material = mfa.confirmEnrollment(code(0))) { assertEquals(10, material.values().length); }
    }
    @Test void administrativeResetOfDisabledAccountPreservesPasswordAndAssignments() {
        UUID second = creation.create("synthetic.second", password.toCharArray(), password.toCharArray()).publicId();
        UUID role = jdbc.queryForObject("SELECT BIN_TO_UUID(public_id) FROM identity_role", (rs, row) -> UUID.fromString(rs.getString(1)));
        membership.assign(second, role);
        enroll();
        Authentication secondAuth = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.second", password));
        SecurityContextHolder.getContext().setAuthentication(secondAuth); proof.reauthenticate(password.toCharArray());
        passwords.reset(id, password.toCharArray(), password.toCharArray());
        assertTrue(jdbc.queryForObject("SELECT mfa_enabled FROM identity_account WHERE canonical_login='synthetic.admin'", Boolean.class));
        String encoding = jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login='synthetic.admin'", String.class);
        jdbc.update("UPDATE identity_account SET enabled=0 WHERE canonical_login='synthetic.admin'");
        mfa.reset(id);
        assertFalse(jdbc.queryForObject("SELECT enabled FROM identity_account WHERE canonical_login='synthetic.admin'", Boolean.class));
        assertNull(jdbc.queryForObject("SELECT mfa_secret FROM identity_account WHERE canonical_login='synthetic.admin'", byte[].class));
        assertTrue(encoding.equals(jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login='synthetic.admin'", String.class)),
                "MFA reset must preserve the stored credential");
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM identity_account_role", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_MFA_RESET'", Integer.class));
    }
    @Test void realHttpLoginProofExpiryAndSecretFreeSession() throws Exception {
        try (Browser old = new Browser()) {
            old.login(null, true); enroll(); assertEquals(302, old.get("/test/csrf").statusCode());
        }
        next();
        try (Browser browser = new Browser()) {
            browser.login(null, false); assertEquals(1, failures());
            browser.login(code(0), true); assertEquals(0, failures());
            assertEquals(403, browser.post("proof", "credential=" + encode(password)).statusCode());
            next(); assertEquals(204, browser.post("proof", "credential=" + encode(password) + "&factor=" + encode(new String(code(0)))).statusCode());
            assertEquals(403, browser.send("disable", "", false).statusCode());
            assertEquals(204, browser.post("disable", "").statusCode());
            assertEquals(302, browser.get("/test/csrf").statusCode());
        }
    }
    @Test void realHttpRecoveryReplayNeverCreatesASecondSession() throws Exception {
        enroll();
        try (Browser one = new Browser(); Browser two = new Browser()) {
            one.login(recovery[0], true); two.login(recovery[0], false);
            assertEquals(200, one.get("/test/csrf").statusCode());
        }
    }
    @Test void authenticationPreparedBeforeMfaRemovalCannotRegisterOverHttp() throws Exception {
        enroll(); next();
        CountDownLatch verified = new CountDownLatch(1), resume = new CountDownLatch(1);
        doAnswer(call -> {
            Authentication result = (Authentication) call.callRealMethod();
            verified.countDown(); assertTrue(resume.await(30, TimeUnit.SECONDS)); return result;
        }).when(provider).authenticate(any());
        try (var pool = Executors.newSingleThreadExecutor(); Browser browser = new Browser()) {
            char[] factor = code(0);
            var login = pool.submit(() -> { browser.login(factor, false); return null; });
            try {
                assertTrue(verified.await(30, TimeUnit.SECONDS));
                fullProof(); mfa.disable();
            } finally { resume.countDown(); }
            login.get(40, TimeUnit.SECONDS);
            assertEquals(302, browser.get("/test/csrf").statusCode());
        } finally { reset(provider); }
    }
    @Test void failedPasswordDoesNotConsumeRecoveryAndFactorFailuresShareLockout() {
        enroll();
        var attempt = UsernamePasswordAuthenticationToken.unauthenticated("synthetic.admin", UUID.randomUUID().toString());
        attempt.setDetails(new MfaInput(recovery[0].clone()));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(attempt));
        assertNull(attempt.getDetails());
        assertEquals(10, jdbc.queryForObject("SELECT mfa_recovery_hashes FROM identity_account", String.class).split(",").length);
        for (int i = 1; i < 5; i++) { assertThrows(BadCredentialsException.class, () -> authenticate(null)); }
        assertEquals(5, failures());
        assertThrows(BadCredentialsException.class, () -> authenticate(recovery[0].clone()));
        assertThrows(RecentAuthenticationException.class, () -> proof.reauthenticate(password.toCharArray(), recovery[0].clone()));
        assertEquals(10, jdbc.queryForObject("SELECT mfa_recovery_hashes FROM identity_account", String.class).split(",").length);
        clock.now = NOW.plusSeconds(901);
        authenticate(recovery[0].clone()); assertEquals(0, failures());
    }
    @Test void durableRecoveryConsumptionSurvivesASeparateApplicationStartup() {
        enroll(); authenticate(recovery[0].clone());
        var application = new org.springframework.boot.builder.SpringApplicationBuilder(HrisApplication.class)
                .properties(Map.of("spring.config.location", "classpath:/", "vaadin.productionMode", "true", "logging.level.root", "OFF",
                        "spring.main.banner-mode", "off", "server.port", "0", "spring.datasource.url", mysql.getJdbcUrl(),
                        "spring.datasource.username", mysql.getUsername(), "spring.datasource.password", mysql.getPassword(),
                        "hris.security.mfa-key-file", KEY.toString()))
                .initializers(context -> context.getEnvironment().getPropertySources().addFirst(
                        new org.springframework.core.env.MapPropertySource("synthetic-restart-key",
                                Map.of("hris.security.mfa-key-file", KEY.toString()))));
        try (var restarted = application.run()) {
            var restartedProvider = restarted.getBean(AccountAuthenticationProvider.class);
            var replay = UsernamePasswordAuthenticationToken.unauthenticated("synthetic.admin", password);
            replay.setDetails(new MfaInput(recovery[0].clone()));
            assertThrows(BadCredentialsException.class, () -> restartedProvider.authenticate(replay));
            var unused = UsernamePasswordAuthenticationToken.unauthenticated("synthetic.admin", password);
            unused.setDetails(new MfaInput(recovery[1].clone()));
            assertTrue(restartedProvider.authenticate(unused).isAuthenticated());
        }
    }
    static char[] otp(byte[] seed, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1"); mac.init(new SecretKeySpec(seed, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[19] & 15;
            int value = ByteBuffer.wrap(hash, offset, 4).getInt() & 0x7fffffff;
            Arrays.fill(hash, (byte) 0);
            return String.format(Locale.ROOT, "%06d", value % 1000000).toCharArray();
        } catch (Exception failure) { throw new IllegalStateException("Test algorithm failed"); }
    }
    static byte[] decode(char[] value) {
        byte[] result = new byte[value.length * 5 / 8]; int bits = 0, buffer = 0, at = 0;
        for (char c : value) {
            int digit = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c); assertTrue(digit >= 0);
            buffer = (buffer << 5) | digit; bits += 5;
            if (bits >= 8) { bits -= 8; result[at++] = (byte) (buffer >>> bits); }
        }
        return result;
    }
    static void cleared(char[] value) { for (char c : value) { assertEquals(0, c, "Owned buffer must clear"); } }
    static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    static final class TestClock extends Clock {
        volatile Instant now = NOW;
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    }
    @TestConfiguration(proxyBeanMethods = false) static class Probes {
        @Bean @Primary TestClock testClock() { return new TestClock(); }
        @Bean static BeanPostProcessor routes() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof HttpSecurity http) {
                        try { http.authorizeHttpRequests(auth -> auth.requestMatchers(request -> request.getRequestURI().startsWith("/test/")).authenticated()); }
                        catch (Exception failure) { throw new IllegalStateException("Test route setup failed"); }
                    } return bean;
                }
            };
        }
        @Bean ServletRegistrationBean<HttpServlet> probe(CredentialReauthenticationService proof, MfaAdministrationService mfa, CurrentActor actor) {
            return new ServletRegistrationBean<>(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
                    actor.requireUserId(); Authentication context = SecurityContextHolder.getContext().getAuthentication();
                    if (context.getCredentials() != null || context.getDetails() != null) { response.setStatus(500); return; }
                    var attrs = request.getSession().getAttributeNames();
                    while (attrs.hasMoreElements()) {
                        Object value = request.getSession().getAttribute(attrs.nextElement());
                        if (value instanceof MfaInput || value instanceof MfaMaterial || value instanceof char[] || value instanceof byte[]) {
                            response.setStatus(500); return;
                        }
                    }
                    response.getWriter().write(((CsrfToken) request.getAttribute(CsrfToken.class.getName())).getToken());
                }
                @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                    try {
                        if ("/proof".equals(request.getPathInfo())) {
                            String factor = request.getParameter("factor");
                            proof.reauthenticate(request.getParameter("credential").toCharArray(), factor == null ? null : factor.toCharArray());
                        } else if ("/disable".equals(request.getPathInfo())) { mfa.disable(); }
                        else { response.setStatus(400); return; }
                        response.setStatus(204);
                    } catch (RecentAuthenticationException | MfaException failure) { response.setStatus(403); }
                }
            }, "/test/*");
        }
    }
    final class Browser implements AutoCloseable {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
        HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> post(String action, String form) throws Exception { return send(action, form, true); }
        HttpResponse<String> send(String action, String form, boolean csrf) throws Exception {
            var request = HttpRequest.newBuilder(uri("/test/" + action)).header("Content-Type", "application/x-www-form-urlencoded");
            if (csrf) { request.header("X-CSRF-TOKEN", get("/test/csrf").body()); }
            return client.send(request.POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        }
        void login(char[] factor, boolean success) throws Exception {
            assertEquals(200, get("/login").statusCode()); String before = cookie();
            String form = "username=synthetic.admin&password=" + encode(password) + (factor == null ? "" : "&factor=" + encode(new String(factor)));
            var response = client.send(HttpRequest.newBuilder(uri("/login")).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.discarding());
            assertEquals(302, response.statusCode());
            assertEquals(success ? 200 : 302, get("/test/csrf").statusCode());
            if (success) { assertFalse(before.equals(cookie()), "Fixation protection must change session ID"); }
            else { assertTrue(response.headers().firstValue("Location").orElseThrow().contains("error")); }
        }
        String cookie() { return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).map(HttpCookie::getValue).findFirst().orElse(""); }
        @Override public void close() { client.close(); }
    }
}

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
@Import({LocalProvisioningIdentityConfiguration.class, AdministrativeTestSessionConfiguration.class, AccountRoleAssignmentIT.ProbeConfiguration.class})
class AccountRoleAssignmentIT {
    @Container @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    private static final Instant NOW = Instant.parse("2026-10-02T04:05:06.123456Z");
    @Autowired private AccountRoleAssignmentService service;
    @Autowired private FirstAdministratorProvisioner bootstrap;
    @Autowired private AccountCreationService creation;
    @MockitoSpyBean private AccountAuthenticationProvider provider;
    @Autowired private CurrentActor actor;
    @Autowired private CredentialReauthenticationService reauthentication;
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
    private UUID role;
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
        reauthentication.reauthenticate(oldPassword.toCharArray());
        targetPassword = UUID.randomUUID().toString();
        target = creation.create("synthetic.target", targetPassword.toCharArray(), targetPassword.toCharArray()).publicId();
        role = UUID.fromString(jdbc.queryForObject("SELECT BIN_TO_UUID(public_id) FROM identity_role WHERE canonical_name='administrator'", String.class));
        clearInvocations(audit, revoker);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void httpAssignRemoveExpiresEveryAffectedSessionAndFreshAuthenticationRefreshesAuthorities() throws Exception {
        try (Browser first = new Browser(); Browser second = new Browser(); Browser third = new Browser();
                Browser administrator = new Browser(); Browser gained = new Browser(); Browser lost = new Browser()) {
            for (Browser browser : List.of(first, second, third)) {
                browser.login("synthetic.target", targetPassword, true);
                assertEquals("false", browser.get("/test/session").body());
            }
            administrator.login("synthetic.self", oldPassword, true);
            administrator.prove(oldPassword);
            var unrelated = tracked(id);
            String before = invariant();
            String other = administratorSnapshot();
            var prior = tracked(target);
            assertEquals(3, prior.size());
            String csrf = administrator.get("/test/csrf").body();
            assertEquals(403, administrator.post("/test/assign?target=" + target + "&role=" + role, null).statusCode());
            assertEquals(0, generation());
            assertEquals(204, administrator.post("/test/assign?target=" + target + "&role=" + role, csrf).statusCode());
            assertEquals(1, generation());
            assertEquals(1, membership(target, role));
            for (var session : prior) { assertTrue(session.isExpired()); }
            for (Browser browser : List.of(first, second, third)) { assertEquals(302, browser.get("/test/session").statusCode()); }
            gained.login("synthetic.target", targetPassword, true);
            assertEquals("true", gained.get("/test/session").body());
            first.login("synthetic.target", targetPassword, true);
            var granted = tracked(target);
            assertEquals(2, granted.size());
            assertEquals(204, administrator.post("/test/remove?target=" + target + "&role=" + role, csrf).statusCode());
            assertEquals(2, generation());
            assertEquals(0, membership(target, role));
            for (var session : granted) { assertTrue(session.isExpired()); }
            assertEquals(302, gained.get("/test/session").statusCode());
            assertEquals(302, first.get("/test/session").statusCode());
            lost.login("synthetic.target", targetPassword, true);
            assertEquals("false", lost.get("/test/session").body());
            for (var session : unrelated) { assertFalse(session.isExpired()); }
            assertEquals("true", administrator.get("/test/session").body());
            assertTrue(before.equals(invariant()));
            assertTrue(other.equals(administratorSnapshot()));
            assertEvents(1, 1);
        }
    }

    @Test
    void realActorDenialMissingNoOpDisabledRoleAndAmbientTransactionPreserveState() {
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service.assign(target, role));
        SecurityContextHolder.getContext().setAuthentication(provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("synthetic.target", targetPassword)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.assign(target, role));
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        reauthentication.reauthenticate(oldPassword.toCharArray());
        bounded(AccountRoleAssignmentException.Reason.ACCOUNT_UNAVAILABLE, () -> service.assign(UUID.randomUUID(), role));
        bounded(AccountRoleAssignmentException.Reason.ROLE_UNAVAILABLE, () -> service.assign(target, UUID.randomUUID()));
        bounded(AccountRoleAssignmentException.Reason.NOT_ASSIGNED, () -> service.remove(target, role));
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                bounded(AccountRoleAssignmentException.Reason.PERSISTENCE_FAILED, () -> service.assign(target, role)));
        UUID disabled = fixtureRole(false, "test:read");
        bounded(AccountRoleAssignmentException.Reason.ROLE_DISABLED, () -> service.assign(target, disabled));
        assertEquals(0, generation());
        verifyNoInteractions(audit, revoker);
        service.assign(target, role);
        clearInvocations(audit, revoker);
        bounded(AccountRoleAssignmentException.Reason.ALREADY_ASSIGNED, () -> service.assign(target, role));
        assertEquals(1, generation());
        verifyNoInteractions(audit, revoker);
        // Fixtures establish an obsolete membership; the production command must permit cleanup.
        jdbc.update("INSERT INTO identity_account_role SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r "
                + "WHERE a.public_id=UUID_TO_BIN(?) AND r.public_id=UUID_TO_BIN(?)", target.toString(), disabled.toString());
        service.remove(target, disabled);
        assertEquals(2, generation());
        assertEquals(1, membership(target, role));
        assertEquals(0, membership(target, disabled));
        assertEvents(1, 1);
    }

    @Test
    void selfAdminRemovalRequiresRemainingEnabledPersistedAuthorityAndHarmlessChangesExpireSelf() throws Exception {
        UUID disabled = fixtureRole(false, "identity:admin");
        jdbc.update("INSERT INTO identity_account_role SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r "
                + "WHERE a.public_id=UUID_TO_BIN(?) AND r.public_id=UUID_TO_BIN(?)", id.toString(), disabled.toString());
        try (Browser administrator = new Browser()) {
            administrator.login("synthetic.self", oldPassword, true);
            administrator.prove(oldPassword);
            var selfSessions = tracked(id);
            bounded(AccountRoleAssignmentException.Reason.SELF_ADMIN_REMOVAL_REJECTED, () -> service.remove(id, role));
            assertEquals(1, membership(id, role));
            assertEquals(0L, jdbc.queryForObject("SELECT authentication_generation FROM identity_account WHERE public_id=UUID_TO_BIN(?)", Long.class, id.toString()));
            assertFalse(selfSessions.getFirst().isExpired());
            verifyNoInteractions(audit, revoker);
            UUID alternate = fixtureRole(true, "identity:admin");
            service.assign(id, alternate);
            assertTrue(selfSessions.getFirst().isExpired());
            assertEquals(302, administrator.get("/test/session").statusCode());
            administrator.login("synthetic.self", oldPassword, true);
            administrator.prove(oldPassword);
            var freshSelf = tracked(id);
            String csrf = administrator.get("/test/csrf").body();
            assertEquals(204, administrator.post("/test/remove?target=" + id + "&role=" + role, csrf).statusCode());
            assertTrue(freshSelf.getFirst().isExpired());
            assertEquals(302, administrator.get("/test/session").statusCode());
            administrator.login("synthetic.self", oldPassword, true);
            administrator.prove(oldPassword);
            assertEquals("true", administrator.get("/test/session").body());
            assertEquals(0, membership(id, role));
            assertEquals(1, membership(id, alternate));
            assertEquals(1, membership(id, disabled));
            bounded(AccountRoleAssignmentException.Reason.SELF_ADMIN_REMOVAL_REJECTED, () -> service.remove(id, alternate));
        }
    }

    @Test
    void assignmentPreservesDisabledAccountTemporaryLockCredentialsAndUnrelatedSecurityState() {
        UUID unrelated = fixtureRole(true, "test:read");
        jdbc.update("INSERT INTO identity_account_role SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r "
                + "WHERE a.public_id=UUID_TO_BIN(?) AND r.public_id=UUID_TO_BIN(?)", target.toString(), unrelated.toString());
        jdbc.update("UPDATE identity_account SET enabled=0,failed_attempts=5,locked_until_utc=? WHERE public_id=UUID_TO_BIN(?)",
                Timestamp.from(NOW.plusSeconds(900)), target.toString());
        String before = invariant();
        String authorization = roleState();
        service.assign(target, role);
        service.remove(target, role);
        assertTrue(before.equals(invariant()));
        assertTrue(authorization.equals(roleState()));
        assertEquals(1, membership(target, unrelated));
        assertEquals(2, generation());
        assertEvents(1, 1);
    }

    @Test
    void auditFailureRollsBackBothDirectionsWithoutRevocation() {
        for (boolean remove : List.of(false, true)) {
            reset(audit);
            if (remove) { service.assign(target, role); }
            clearInvocations(revoker);
            String before = snapshot();
            doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("Synthetic audit failure"); }).when(audit).record(any());
            bounded(AccountRoleAssignmentException.Reason.PERSISTENCE_FAILED, () -> change(remove));
            assertTrue(before.equals(snapshot()));
            assertEvents(remove ? 1 : 0, 0);
            verifyNoInteractions(revoker);
        }
    }

    @Test
    void mutationFlushFailureRollsBackMembershipGenerationAuditBeforeRevocation() {
        String before = snapshot();
        jdbc.execute("CREATE TRIGGER synthetic_assignment_failure BEFORE INSERT ON identity_account_role FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic mutation failure'");
        try {
            bounded(AccountRoleAssignmentException.Reason.PERSISTENCE_FAILED, () -> service.assign(target, role));
            assertTrue(before.equals(snapshot()));
            assertEvents(0, 0);
            verifyNoInteractions(revoker);
        } finally { jdbc.execute("DROP TRIGGER synthetic_assignment_failure"); }
    }

    @Test
    void auditInsertFailureRollsBackWithoutRevocation() {
        String before = snapshot();
        jdbc.execute("CREATE TRIGGER synthetic_assignment_audit_failure BEFORE INSERT ON audit_event FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic audit insertion failure'");
        try {
            bounded(AccountRoleAssignmentException.Reason.PERSISTENCE_FAILED, () -> service.assign(target, role));
            assertTrue(before.equals(snapshot()));
            assertEvents(0, 0);
            verifyNoInteractions(revoker);
        } finally { jdbc.execute("DROP TRIGGER synthetic_assignment_audit_failure"); }
    }

    @Test
    void revocationFailureRollsBackBothDirectionsAfterMutationAndAuditFlush() {
        for (boolean remove : List.of(false, true)) {
            reset(revoker);
            if (remove) { service.assign(target, role); }
            String before = snapshot();
            SessionInformation session = trackTarget();
            long previous = generation();
            doAnswer(call -> {
                assertEquals(previous + 1, generation());
                assertEquals(remove ? 0 : 1, membership(target, role));
                assertEvents(1, remove ? 1 : 0); // Writes are already visible in the mutation transaction.
                call.callRealMethod();
                throw new IllegalStateException("Synthetic expiry failure");
            }).when(revoker).revoke(target);
            bounded(AccountRoleAssignmentException.Reason.SESSION_REVOCATION_FAILED, () -> change(remove));
            assertTrue(before.equals(snapshot()));
            assertTrue(session.isExpired());
            assertEvents(remove ? 1 : 0, 0);
        }
    }

    @Test
    void lateCommitFailureRetainsPreviousMembershipWhileSessionsRemainExpiredForBothDirections() {
        for (boolean remove : List.of(false, true)) {
            reset(revoker);
            if (remove) { service.assign(target, role); }
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
            bounded(AccountRoleAssignmentException.Reason.PERSISTENCE_FAILED, () -> change(remove));
            assertTrue(before.equals(snapshot()));
            assertTrue(session.isExpired());
            assertEvents(remove ? 1 : 0, 0);
            assertEquals(remove, hasAdmin(provider.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated("synthetic.target", targetPassword))));
        }
    }

    @Test
    void servletBoundStaleMembershipAndGenerationAreRefreshedUnderAccountLock() {
        var entities = entityManagerFactory.createEntityManager();
        try {
            Object account = entities.createQuery("select account from AccountEntity account left join fetch account.roles where account.publicId=:id")
                    .setParameter("id", target).getSingleResult();
            service.assign(target, role);
            TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entities));
            service.remove(target, role);
            assertEquals(0, membership(target, role));
            assertEquals(2, generation());
            assertEvents(1, 1);
            assertNotNull(account);
        } finally {
            TransactionSynchronizationManager.unbindResourceIfPossible(entityManagerFactory);
            entities.close();
        }
    }

    @Test void assignmentFirstRejectsOldHttpAuthenticationAtFinalRegistration() throws Exception { delayedAuthentication(false, false); }
    @Test void removalFirstRejectsOldHttpAuthenticationAtFinalRegistration() throws Exception { delayedAuthentication(true, false); }
    @Test void assignThenRemoveNeverReusesOldAuthenticationGeneration() throws Exception { delayedAuthentication(false, true); }

    private void delayedAuthentication(boolean remove, boolean reverse) throws Exception {
        if (remove) { service.assign(target, role); }
        var verified = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        try (Browser delayed = new Browser(); Browser fresh = new Browser(); Browser unrelated = new Browser();
                var executor = Executors.newSingleThreadExecutor()) {
            reset(provider);
            unrelated.login("synthetic.self", oldPassword, true);
            // Install the pause only for the affected login, after establishing an unrelated session.
            doAnswer(call -> {
                Authentication result = (Authentication) call.callRealMethod();
                verified.countDown();
                assertTrue(resume.await(30, TimeUnit.SECONDS), "Registration must be released");
                return result;
            }).when(provider).authenticate(any());
            var login = executor.submit(() -> { delayed.login("synthetic.target", targetPassword, false); return null; });
            try {
                assertTrue(verified.await(30, TimeUnit.SECONDS), "Authentication must finish before assignment change");
                assertTrue(tracked(target).isEmpty());
                change(remove); // Completes under the real Account lock before final registration.
                if (reverse) { change(!remove); }
                resume.countDown();
                login.get(30, TimeUnit.SECONDS);
                assertTrue(tracked(target).isEmpty());
                assertEquals("true", unrelated.get("/test/session").body());
                reset(provider);
                fresh.login("synthetic.target", targetPassword, true);
                assertEquals(Boolean.toString(!remove && !reverse), fresh.get("/test/session").body());
                assertEvents(1, remove || reverse ? 1 : 0);
            } finally { resume.countDown(); }
        }
    }

    @Test void registrationFirstAssignmentExpiresSessionBeforeContextPersistence() throws Exception { registrationFirst(false); }
    @Test void registrationFirstRemovalExpiresSessionBeforeContextPersistence() throws Exception { registrationFirst(true); }

    private void registrationFirst(boolean remove) throws Exception {
        if (remove) { service.assign(target, role); }
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
                change(remove);
                assertTrue(session.isExpired());
                resume.countDown();
                login.get(30, TimeUnit.SECONDS);
                assertEquals(302, delayed.get("/test/session").statusCode());
                assertEquals(302, delayed.get("/test/session").statusCode());
                assertTrue(tracked(target).isEmpty());
                assertEvents(1, remove ? 1 : 0);
            } finally { resume.countDown(); }
        }
    }

    @Test
    void concurrentDuplicateAssignmentSerializesAndAdvancesGenerationOnlyOnce() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        doAnswer(call -> {
            locked.countDown();
            assertTrue(release.await(30, TimeUnit.SECONDS));
            return call.callRealMethod();
        }).when(revoker).revoke(target);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { withActor(() -> service.assign(target, role)); return null; });
            try {
                assertTrue(locked.await(30, TimeUnit.SECONDS));
                var second = executor.submit(() -> {
                    secondStarted.countDown();
                    withActor(() -> bounded(AccountRoleAssignmentException.Reason.ALREADY_ASSIGNED, () -> service.assign(target, role)));
                    return null;
                });
                assertTrue(secondStarted.await(30, TimeUnit.SECONDS));
                release.countDown();
                first.get(30, TimeUnit.SECONDS);
                second.get(30, TimeUnit.SECONDS);
                assertEquals(1, generation());
                assertEquals(1, membership(target, role));
                assertEvents(1, 0);
                verify(revoker).revoke(target);
            } finally { release.countDown(); }
        }
    }

    private void withActor(Runnable command) {
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        reauthentication.reauthenticate(oldPassword.toCharArray());
        try { command.run(); } finally { SecurityContextHolder.clearContext(); }
    }
    private void change(boolean remove) { if (remove) { service.remove(target, role); } else { service.assign(target, role); } }
    private static boolean hasAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(value -> "identity:admin".equals(value.getAuthority()));
    }
    private UUID fixtureRole(boolean enabled, String authority) {
        UUID result = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_role(public_id,canonical_name,enabled) VALUES(UUID_TO_BIN(?),?,?)",
                result.toString(), "synthetic." + result, enabled);
        jdbc.update("INSERT IGNORE INTO identity_permission(authority_key) VALUES(?)", authority);
        jdbc.update("INSERT INTO identity_role_permission SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p "
                + "WHERE r.public_id=UUID_TO_BIN(?) AND p.authority_key=?", result.toString(), authority);
        return result;
    }
    private int membership(UUID account, UUID assignedRole) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM identity_account_role ar JOIN identity_account a ON a.id=ar.account_id "
                + "JOIN identity_role r ON r.id=ar.role_id WHERE a.public_id=UUID_TO_BIN(?) AND r.public_id=UUID_TO_BIN(?)",
                Integer.class, account.toString(), assignedRole.toString());
    }
    private String snapshot() {
        return jdbc.queryForMap("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,"
                + "locked_until_utc,credential_updated_at_utc,authentication_generation,security_updated_at_utc,row_version "
                + "FROM identity_account WHERE canonical_login='synthetic.target'").toString()
                + jdbc.queryForList("SELECT * FROM identity_account_role ORDER BY account_id,role_id");
    }
    private String invariant() {
        return jdbc.queryForMap("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,locked_until_utc,"
                + "credential_updated_at_utc,security_updated_at_utc FROM identity_account WHERE canonical_login='synthetic.target'").toString();
    }
    private String roleState() {
        return jdbc.queryForList("SELECT id,HEX(public_id),canonical_name,enabled,row_version FROM identity_role ORDER BY id").toString()
                + jdbc.queryForList("SELECT * FROM identity_permission ORDER BY id")
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
    private void assertEvents(int assigned, int removed) {
        assertEquals(assigned, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_ACCOUNT_ROLE_ASSIGNED'", Integer.class));
        assertEquals(removed, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_ACCOUNT_ROLE_REMOVED'", Integer.class));
        var events = jdbc.queryForList("SELECT actor_reference,target_type,target_reference,reason,context FROM audit_event "
                + "WHERE action IN ('IDENTITY_ACCOUNT_ROLE_ASSIGNED','IDENTITY_ACCOUNT_ROLE_REMOVED')");
        for (var event : events) {
            assertEquals(id.toString(), event.get("actor_reference"));
            assertEquals(target + "/" + (event.get("target_reference").toString().split("/")[1]), event.get("target_reference"));
            assertEquals("IDENTITY_ACCOUNT_ROLE", event.get("target_type"));
            String reference = event.get("target_reference").toString();
            String[] ids = reference.split("/");
            assertEquals(2, ids.length);
            assertEquals(UUID.fromString(ids[0]).toString() + "/" + UUID.fromString(ids[1]), reference);
            assertEquals("administrative-account-role-assignment", event.get("context"));
            assertNull(event.get("reason"));
            assertFalse(event.toString().contains(targetPassword));
            assertFalse(event.toString().contains("synthetic."));
        }
        flyway.validate();
    }
    private static void bounded(AccountRoleAssignmentException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(AccountRoleAssignmentException.class, action);
        assertEquals(reason, failure.reason());
        assertEquals("Account role assignment failed: " + reason, failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
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
        @Bean @Primary Clock assignmentTestClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean ServletRegistrationBean<HttpServlet> assignmentProbe(
                AccountRoleAssignmentService service, CurrentActor actor, CredentialReauthenticationService reauthentication) {
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
                        response.getWriter().write(Boolean.toString(actor.hasAuthority("identity:admin")));
                    }
                }
                @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                    if (request.getPathInfo().equals("/reauthenticate")) {
                        reauthentication.reauthenticate(request.getParameter("credential").toCharArray());
                        response.setStatus(204); return;
                    }
                    UUID target = UUID.fromString(request.getParameter("target"));
                    UUID role = UUID.fromString(request.getParameter("role"));
                    if (request.getPathInfo().equals("/assign")) { service.assign(target, role); }
                    else if (request.getPathInfo().equals("/remove")) { service.remove(target, role); }
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
        void prove(String password) throws Exception {
            String csrf = get("/test/csrf").body();
            String form = "credential=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
            var response = client.send(HttpRequest.newBuilder(uri("/test/reauthenticate"))
                    .header("X-CSRF-TOKEN", csrf).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.discarding());
            assertEquals(204, response.statusCode());
        }
        String sessionCookie() {
            return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("JSESSIONID"))
                    .map(java.net.HttpCookie::getValue).findFirst().orElse("");
        }
        URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
        @Override public void close() { client.close(); }
    }
}

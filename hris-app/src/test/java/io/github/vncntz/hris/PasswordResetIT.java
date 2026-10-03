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
@Import({LocalProvisioningIdentityConfiguration.class, AdministrativeTestSessionConfiguration.class, PasswordResetIT.ProbeConfiguration.class})
class PasswordResetIT {
    @Container @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    private static final Instant NOW = Instant.parse("2026-10-02T04:05:06.123456Z");
    @Autowired private PasswordResetService service;
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
    @MockitoSpyBean private PasswordEncoder encoder;
    @MockitoSpyBean private AuthenticatedSessionRevoker revoker;
    @Value("${local.server.port}") private int port;
    private UUID id;
    private UUID administrator;
    private String adminPassword;
    private Authentication administrative;
    @Autowired private PasswordChangeService selfChange;
    @Autowired private AccountLifecycleService lifecycle;
    @Autowired private RecentAuthenticationSession recentSession;
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
        adminPassword = UUID.randomUUID().toString();
        administrator = bootstrap.provision("synthetic.admin", adminPassword.toCharArray(), adminPassword.toCharArray());
        administrative = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.admin", adminPassword));
        SecurityContextHolder.getContext().setAuthentication(administrative);
        reauthentication.reauthenticate(adminPassword.toCharArray());
        id = creation.create("synthetic.self", oldPassword.toCharArray(), oldPassword.toCharArray()).publicId();
        authenticated = authenticate(oldPassword);
        clearInvocations(audit, encoder, revoker);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorityProofSelfMissingAndAmbientDenialsPreserveState() {
        String before = snapshot("synthetic.self");
        SecurityContextHolder.clearContext();
        char[][] anonymous = buffers(UUID.randomUUID().toString());
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> invoke(anonymous));
        cleared(anonymous);
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        char[][] ordinary = buffers(UUID.randomUUID().toString());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> invoke(ordinary));
        cleared(ordinary);
        SecurityContextHolder.getContext().setAuthentication(administrative);
        recentSession.attempt(() -> new RecentAuthenticationSession.Proof(administrator, NOW.minusSeconds(300)));
        char[][] expired = buffers(UUID.randomUUID().toString());
        assertThrows(RecentAuthenticationException.class, () -> invoke(expired));
        cleared(expired);
        // A marker for another Account cannot provide administrator credential freshness.
        recentSession.attempt(() -> new RecentAuthenticationSession.Proof(id, NOW));
        assertThrows(RecentAuthenticationException.class, () -> change(UUID.randomUUID().toString()));
        reauthentication.reauthenticate(adminPassword.toCharArray());
        char[][] own = buffers(UUID.randomUUID().toString());
        bounded(PasswordResetException.Reason.SELF_RESET_REJECTED,
                () -> service.reset(administrator, own[0], own[1]));
        cleared(own);
        char[][] missing = buffers(UUID.randomUUID().toString());
        bounded(PasswordResetException.Reason.ACCOUNT_UNAVAILABLE,
                () -> service.reset(UUID.randomUUID(), missing[0], missing[1]));
        cleared(missing);
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                bounded(PasswordResetException.Reason.PERSISTENCE_FAILED, () -> change(UUID.randomUUID().toString())));
        assertTrue(before.equals(snapshot("synthetic.self")));
        verify(encoder, never()).encode(any()); verifyNoInteractions(audit, revoker);
        assertPreserved(1, 1);
    }

    @Test
    void disabledTargetResetPreservesDisablementAndAssignmentsAndClearsLock() {
        jdbc.update("UPDATE identity_account SET enabled=0,failed_attempts=5,locked_until_utc=? WHERE canonical_login='synthetic.self'",
                Timestamp.from(NOW.plusSeconds(900)));
        String administratorBefore = snapshot("synthetic.admin");
        String replacement = UUID.randomUUID().toString();
        change(replacement);
        assertEquals(0, jdbc.queryForObject("SELECT enabled FROM identity_account WHERE canonical_login='synthetic.self'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT failed_attempts FROM identity_account WHERE canonical_login='synthetic.self'", Integer.class));
        assertNull(jdbc.queryForObject("SELECT locked_until_utc FROM identity_account WHERE canonical_login='synthetic.self'", Timestamp.class));
        assertEquals(1L, generation()); assertTrue(administratorBefore.equals(snapshot("synthetic.admin")));
        assertThrows(BadCredentialsException.class, () -> authenticate(replacement));
        assertTrue(encoder.matches(replacement, encoding()));
        assertFalse(encoder.matches(oldPassword, encoding()));
        assertEvent(1); assertPreserved(1, 2);
        lifecycle.enable(id);
        assertTrue(authenticate(replacement).isAuthenticated());
        assertThrows(BadCredentialsException.class, () -> authenticate(oldPassword));
    }

    @Test
    void servletBoundSnapshotIsRefreshedBeforeGenerationRecheck() {
        var entities = entityManagerFactory.createEntityManager();
        try {
            entities.createQuery("select account from AccountEntity account where account.publicId=:id")
                    .setParameter("id", id).getSingleResult();
            TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entities));
            doAnswer(call -> {
                jdbc.update("UPDATE identity_account SET authentication_generation=authentication_generation+1 WHERE canonical_login='synthetic.self'");
                return call.callRealMethod();
            }).when(encoder).encode(any());
            String before = encoding();
            bounded(PasswordResetException.Reason.STALE_ACCOUNT, () -> change(UUID.randomUUID().toString()));
            assertTrue(before.equals(encoding())); assertEquals(1L, generation());
            verifyNoInteractions(audit, revoker); assertPreserved(1, 1);
        } finally {
            TransactionSynchronizationManager.unbindResourceIfPossible(entityManagerFactory);
            entities.close();
        }
    }

    @Test
    void concurrentResetRejectsStaleResetAndSelfChangeCannotRestoreOldCredential() throws Exception {
        concurrentMutation(false);
    }

    @Test
    void selfChangeCommittedDuringResetEncodingRejectsStaleReset() throws Exception {
        concurrentMutation(true);
    }

    private void concurrentMutation(boolean selfWins) throws Exception {
        var encodingStarted = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        var encodingOrder = new AtomicInteger();
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            if (encodingOrder.getAndIncrement() == 0) {
                encodingStarted.countDown();
                assertTrue(resume.await(30, TimeUnit.SECONDS));
            }
            return call.callRealMethod();
        }).when(encoder).encode(any());
        String stalePassword = UUID.randomUUID().toString(), winner = UUID.randomUUID().toString();
        try (var workers = Executors.newSingleThreadExecutor()) {
            var stale = workers.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(administrative);
                reauthentication.reauthenticate(adminPassword.toCharArray());
                try { change(stalePassword); return null; }
                catch (PasswordResetException failure) { return failure.reason(); }
                finally { SecurityContextHolder.clearContext(); }
            });
            try {
                assertTrue(encodingStarted.await(30, TimeUnit.SECONDS));
                if (selfWins) {
                    SecurityContextHolder.getContext().setAuthentication(authenticated);
                    selfChange.change(oldPassword.toCharArray(), winner.toCharArray(), winner.toCharArray());
                    SecurityContextHolder.getContext().setAuthentication(administrative);
                } else { change(winner); }
                resume.countDown();
                assertEquals(PasswordResetException.Reason.STALE_ACCOUNT, stale.get(30, TimeUnit.SECONDS));
            } finally { resume.countDown(); }
        }
        reset(encoder);
        assertTrue(authenticate(winner).isAuthenticated());
        assertThrows(BadCredentialsException.class, () -> authenticate(stalePassword));
        assertThrows(BadCredentialsException.class, () -> authenticate(oldPassword));
        assertEquals(1L, generation()); assertPreserved(1, 2);
        assertEquals(selfWins ? 0 : 1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_PASSWORD_RESET'", Integer.class));
    }

    @Test
    void resetDuringSelfChangeComparisonRejectsStaleSelfChange() throws Exception {
        var compared = new CountDownLatch(1); var resume = new CountDownLatch(1);
        String original = encoding();
        doAnswer(call -> {
            Object result = call.callRealMethod();
            if (original.equals(call.getArgument(1))) {
                compared.countDown(); assertTrue(resume.await(30, TimeUnit.SECONDS));
            }
            return result;
        }).when(encoder).matches(any(), any());
        String winner = UUID.randomUUID().toString(), stale = UUID.randomUUID().toString();
        try (var workers = Executors.newSingleThreadExecutor()) {
            var change = workers.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(authenticated);
                try {
                    selfChange.change(oldPassword.toCharArray(), stale.toCharArray(), stale.toCharArray());
                    return null;
                } catch (PasswordChangeException failure) { return failure.reason(); }
                finally { SecurityContextHolder.clearContext(); }
            });
            try {
                assertTrue(compared.await(30, TimeUnit.SECONDS));
                change(winner); resume.countDown();
                assertEquals(PasswordChangeException.Reason.STALE_CREDENTIAL, change.get(30, TimeUnit.SECONDS));
            } finally { resume.countDown(); }
        }
        reset(encoder); assertTrue(authenticate(winner).isAuthenticated());
        assertThrows(BadCredentialsException.class, () -> authenticate(oldPassword));
        assertThrows(BadCredentialsException.class, () -> authenticate(stale));
        assertEvent(1); assertEquals(1L, generation());
    }

    @Test
    void allTargetSessionsExpireWhileAdministratorAndUnrelatedSessionRemainUsable() throws Exception {
        try (Browser first = new Browser(); Browser second = new Browser(); Browser admin = new Browser(); Browser fresh = new Browser()) {
            first.login("synthetic.self", oldPassword, true); second.login("synthetic.self", oldPassword, true);
            admin.login("synthetic.admin", adminPassword, true);
            String replacement = UUID.randomUUID().toString(); change(replacement);
            assertEquals(2, tracked(id).size());
            for (var session : tracked(id)) { assertTrue(session.isExpired()); }
            assertEquals(302, first.get("/test/session").statusCode());
            assertEquals(302, second.get("/test/session").statusCode());
            assertEquals(200, admin.get("/test/session").statusCode());
            fresh.login("synthetic.self", replacement, true);
            assertEvent(1);
        }
    }

    private long generation() {
        return jdbc.queryForObject("SELECT authentication_generation FROM identity_account WHERE canonical_login='synthetic.self'", Long.class);
    }

    @Test
    void repeatedResetAtFixedClockCannotReuseAnEarlierAuthenticationGeneration() {
        String first = UUID.randomUUID().toString(); change(first);
        Authentication intermediate = authenticate(first);
        assertEquals(1L, generation());
        String second = UUID.randomUUID().toString(); change(second);
        assertEquals(2L, generation());
        assertEquals(java.time.LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), jdbc.queryForObject(
                "SELECT credential_updated_at_utc FROM identity_account WHERE canonical_login='synthetic.self'", java.time.LocalDateTime.class));
        var registrations = new AtomicInteger();
        assertThrows(org.springframework.security.web.authentication.session.SessionAuthenticationException.class,
                () -> registration.register(intermediate, registrations::incrementAndGet));
        assertEquals(0, registrations.get()); assertNull(intermediate.getDetails());
        assertTrue(authenticate(second).isAuthenticated());
        assertThrows(BadCredentialsException.class, () -> authenticate(first));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_PASSWORD_RESET'", Integer.class));
    }

    @Test
    void successfulReplacementPreservesAssignmentsBootstrapUnrelatedAccountAndSchema() {
        jdbc.update("INSERT INTO identity_account_role(account_id,role_id) SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r WHERE a.canonical_login='synthetic.self'");
        authenticated = authenticate(oldPassword);
        var memberships = jdbc.queryForList("SELECT account_id,role_id FROM identity_account_role ORDER BY account_id,role_id");
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
        assertPreserved(2, 3, 2);
        assertEquals(memberships, jdbc.queryForList("SELECT account_id,role_id FROM identity_account_role ORDER BY account_id,role_id"));
        assertThrows(BadCredentialsException.class, () -> authenticate(oldPassword));
        var fresh = authenticate(replacement);
        assertEquals(id, ((AccountPrincipal)fresh.getPrincipal()).publicId());
        assertEquals(authenticated.getAuthorities(), fresh.getAuthorities());
        assertTrue(fresh.getAuthorities().stream().anyMatch(value -> value.getAuthority().equals("identity:admin")));
        var unrelated = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.other", otherPassword));
        assertEquals(other.publicId(), ((AccountPrincipal)unrelated.getPrincipal()).publicId());
        assertTrue(unrelated.getAuthorities().isEmpty());
        flyway.validate();
        assertEquals(8, count("flyway_schema_history"));
    }

    @Test
    void auditInsertThenFailureRollsBackPasswordAndAuditWithoutExpiry() {
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("Synthetic audit failure");
        }).when(audit).record(any());
        assertRollback(PasswordResetException.Reason.PERSISTENCE_FAILED);
        verifyNoInteractions(revoker);
    }

    @Test
    void databaseFlushFailureRollsBackAuditAndCredentialBeforeExpiry() {
        jdbc.execute("CREATE TRIGGER synthetic_password_failure BEFORE UPDATE ON identity_account FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic mutation failure'");
        try {
            assertRollback(PasswordResetException.Reason.PERSISTENCE_FAILED);
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
        assertRollback(PasswordResetException.Reason.SESSION_REVOCATION_FAILED);
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
        assertRollback(PasswordResetException.Reason.PERSISTENCE_FAILED);
        assertTrue(session.isExpired());
        authenticate(oldPassword);
    }

    @Test
    void oldCredentialAuthenticationPausedBeforeRegistrationCannotCreateSessionAfterChange() throws Exception {
        var verified = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        doAnswer(call -> {
            Authentication result = (Authentication) call.callRealMethod();
            verified.countDown();
            assertTrue(resume.await(30, TimeUnit.SECONDS), "Registration must be released");
            return result;
        }).when(provider).authenticate(any());
        try (Browser delayed = new Browser(); Browser fresh = new Browser();
                var executor = Executors.newSingleThreadExecutor()) {
            var login = executor.submit(() -> { delayed.login("synthetic.self", oldPassword, false); return null; });
            try {
                assertTrue(verified.await(30, TimeUnit.SECONDS), "Credential authentication must complete first");
                assertEquals(0, tracked(id).size());
                String replacement = UUID.randomUUID().toString();
                change(replacement);
                assertEvent(1);
                resume.countDown();
                login.get(30, TimeUnit.SECONDS);
                assertEquals(0, tracked(id).size());
                reset(provider);
                fresh.login("synthetic.self", replacement, true);
            } finally { resume.countDown(); }
        }
    }

    @Test
    void registrationCompletingFirstIsRevokedEvenBeforeSecurityContextPersistence() throws Exception {
        var registered = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        doAnswer(call -> {
            call.callRealMethod(); // Real transaction/row lock and real servlet registry registration complete.
            registered.countDown();
            assertTrue(resume.await(30, TimeUnit.SECONDS), "Context persistence must be released");
            return null;
        }).when(registration).register(any(), any());
        try (Browser delayed = new Browser(); Browser fresh = new Browser();
                var executor = Executors.newSingleThreadExecutor()) {
            var login = executor.submit(() -> { delayed.login("synthetic.self", oldPassword, true, false); return null; });
            try {
                assertTrue(registered.await(30, TimeUnit.SECONDS), "Registration must finish first");
                assertEquals(1, tracked(id).size());
                var prior = tracked(id).getFirst();
                assertFalse(prior.isExpired());
                String replacement = UUID.randomUUID().toString();
                change(replacement);
                assertTrue(prior.isExpired());
                resume.countDown();
                login.get(30, TimeUnit.SECONDS);
                assertEquals(302, delayed.get("/test/session").statusCode());
                assertEquals(302, delayed.get("/test/session").statusCode());
                assertEquals(0, tracked(id).size());
                reset(registration);
                fresh.login("synthetic.self", replacement, true);
                assertEvent(1);
            } finally { resume.countDown(); }
        }
    }

    @Test
    void ordinaryConcurrentLoginsWithSameGenerationBothRegisterAndKeepUnrelatedSessions() throws Exception {
        String otherPassword = UUID.randomUUID().toString();
        UUID other = creation.create("synthetic.other", otherPassword.toCharArray(), otherPassword.toCharArray()).publicId();
        try (Browser unrelated = new Browser(); Browser first = new Browser(); Browser second = new Browser();
                var executor = Executors.newFixedThreadPool(2)) {
            unrelated.login("synthetic.other", otherPassword, true);
            var verified = new CountDownLatch(2);
            var resume = new CountDownLatch(1);
            doAnswer(call -> {
                Authentication result = (Authentication) call.callRealMethod();
                verified.countDown();
                assertTrue(resume.await(30, TimeUnit.SECONDS), "Concurrent registrations must be released");
                return result;
            }).when(provider).authenticate(any());
            var one = executor.submit(() -> { first.login("synthetic.self", oldPassword, true); return null; });
            var two = executor.submit(() -> { second.login("synthetic.self", oldPassword, true); return null; });
            try {
                assertTrue(verified.await(30, TimeUnit.SECONDS), "Both credential authentications must finish");
                resume.countDown();
                one.get(30, TimeUnit.SECONDS); two.get(30, TimeUnit.SECONDS);
                assertEquals(2, tracked(id).size());
                for (var session : tracked(id)) { assertFalse(session.isExpired()); }
                change(UUID.randomUUID().toString());
                assertEquals(302, first.get("/test/session").statusCode());
                assertEquals(302, second.get("/test/session").statusCode());
                assertEquals(200, unrelated.get("/test/session").statusCode());
                assertEquals(1, tracked(other).size());
                assertFalse(tracked(other).getFirst().isExpired());
            } finally { resume.countDown(); }
        }
    }

    @Test
    void realHttpRegistrationCommitFailureRemovesEntryAndCannotPersistAuthenticatedContext() throws Exception {
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                    "Real registry registration must occur inside the locked transaction");
            call.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("Synthetic registration commit failure");
                }
            });
            return null;
        }).when(registry).registerNewSession(anyString(), any());
        try (Browser rejected = new Browser(); Browser retry = new Browser()) {
            rejected.login("synthetic.self", oldPassword, false);
            assertEquals(0, tracked(id).size());
            assertEquals(302, rejected.get("/test/session").statusCode());
            reset(registry);
            retry.login("synthetic.self", oldPassword, true);
        }
    }

    private void assertRollback(PasswordResetException.Reason reason) {
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
        return new char[][]{replacement.toCharArray(), replacement.toCharArray()};
    }

    private void invoke(char[][] buffers) { service.reset(id, buffers[0], buffers[1]); }
    private Authentication authenticate(String password) {
        return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("synthetic.self", password));
    }
    private String encoding() {
        return jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login='synthetic.self'", String.class);
    }
    private String snapshot(String login) {
        return jdbc.queryForMap("SELECT id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,"
                + "locked_until_utc,credential_updated_at_utc,authentication_generation,security_updated_at_utc,row_version "
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
        assertPreserved(accounts, events, 1);
    }
    private void assertPreserved(int accounts, int events, int memberships) {
        assertEquals(accounts + 1, count("identity_account")); assertEquals(events + 1, count("audit_event"));
        assertEquals(memberships, count("identity_account_role"));
        for (String table : List.of("identity_role", "identity_permission", "identity_role_permission")) {
            assertEquals(1, count(table));
        }
        assertEquals(1, jdbc.queryForObject("SELECT completed FROM identity_bootstrap_state", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='FIRST_ADMINISTRATOR_PROVISIONED'", Integer.class));
        flyway.validate();
        assertEquals(8, count("flyway_schema_history"));
    }
    private void assertEvent(int expected) {
        assertEquals(expected, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_PASSWORD_RESET'", Integer.class));
        var event = jdbc.queryForMap("SELECT actor_reference,target_type,target_reference,reason,context FROM audit_event WHERE action='IDENTITY_PASSWORD_RESET'");
        assertEquals(administrator.toString(), event.get("actor_reference"));
        assertEquals(id.toString(), event.get("target_reference"));
        assertEquals("IDENTITY_ACCOUNT", event.get("target_type"));
        assertEquals("administrative-password-reset", event.get("context"));
        assertNull(event.get("reason"));
        assertFalse(event.toString().contains(oldPassword));
        assertFalse(event.toString().contains(encoding()));
    }
    private static void bounded(PasswordResetException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(PasswordResetException.class, action);
        assertEquals(reason, failure.reason());
        assertNull(failure.getCause());
        assertEquals("Password reset failed: " + reason, failure.getMessage());
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
        @Bean @Primary Clock passwordResetTestClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean ServletRegistrationBean<HttpServlet> passwordResetProbe(CurrentActor actor) {
            return new ServletRegistrationBean<>(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws java.io.IOException {
                    actor.requireUserId();
                    Authentication context = SecurityContextHolder.getContext().getAuthentication();
                    if (context.getCredentials() != null || context.getDetails() != null) {
                        response.setStatus(500); return;
                    }
                    if (request.getSession().getMaxInactiveInterval() != 1800) { response.setStatus(500); return; }
                    response.getWriter().write("authenticated");
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

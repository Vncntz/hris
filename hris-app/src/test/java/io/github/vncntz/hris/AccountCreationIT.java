package io.github.vncntz.hris;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.github.vncntz.hris.identityaccess.*;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"vaadin.productionMode=true", "logging.level.root=OFF"})
@Import(LocalProvisioningIdentityConfiguration.class)
class AccountCreationIT {
    @Container @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    @Autowired private AccountCreationService service;
    @Autowired private FirstAdministratorProvisioner bootstrap;
    @Autowired private AccountAuthenticationProvider provider;
    @Autowired private CurrentActor actor;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Flyway flyway;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoSpyBean private AuditRecorder audit;
    @MockitoSpyBean private PasswordEncoder encoder;
    private Authentication administrator;
    private UUID creator;

    @BeforeEach
    void freshBootstrapAndRealAuthentication() {
        Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .cleanDisabled(false).load().clean();
        flyway.migrate();
        String secret = UUID.randomUUID().toString();
        creator = bootstrap.provision("synthetic.creator", secret.toCharArray(), secret.toCharArray());
        administrator = authenticate("synthetic.creator", secret);
        SecurityContextHolder.getContext().setAuthentication(administrator);
        actor.requireAuthority("identity:admin");
        clearInvocations(audit, encoder);
    }

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void firstAdministratorCreatesOrdinaryAccountWithStableIdentityAndAtomicPrivateAudit() {
        String secret = "  " + UUID.randomUUID() + "  ";
        char[] password = secret.toCharArray(), confirmation = password.clone();
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return call.callRealMethod();
        }).when(encoder).encode(any());
        CreatedAccount created = service.create(" SYNTHETIC.ORDINARY ", password, confirmation);
        cleared(password, confirmation);
        assertTrue(Arrays.equals(bytes(created.publicId()), jdbc.queryForObject(
                "SELECT public_id FROM identity_account WHERE canonical_login='synthetic.ordinary'", byte[].class)));
        var state = jdbc.queryForMap("SELECT enabled,failed_attempts,locked_until_utc,row_version,"
                + "credential_updated_at_utc,security_updated_at_utc FROM identity_account WHERE canonical_login='synthetic.ordinary'");
        assertEquals(Boolean.TRUE, state.get("enabled"));
        assertEquals(0, ((Number)state.get("failed_attempts")).intValue());
        assertEquals(0, ((Number)state.get("row_version")).intValue());
        assertNull(state.get("locked_until_utc"));
        assertEquals(state.get("credential_updated_at_utc"), state.get("security_updated_at_utc"));
        String hash = jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login='synthetic.ordinary'", String.class);
        assertTrue(hash.startsWith("{argon2@SpringSecurity_v5_8}$argon2id$"));
        assertTrue(encoder.matches(secret, hash));
        assertFalse(encoder.matches(secret.strip(), hash));
        assertIdentityCounts(2, 2);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM identity_account_role ar JOIN identity_account a ON a.id=ar.account_id WHERE a.canonical_login='synthetic.ordinary'", Integer.class));
        var event = jdbc.queryForMap("SELECT actor_reference,action,target_type,target_reference,reason,context FROM audit_event WHERE action='IDENTITY_ACCOUNT_CREATED'");
        assertEquals(creator.toString(), event.get("actor_reference"));
        assertEquals("IDENTITY_ACCOUNT", event.get("target_type"));
        assertEquals(created.publicId().toString(), event.get("target_reference"));
        assertEquals("IDENTITY_ACCOUNT_CREATED", event.get("action"));
        assertEquals("authenticated-account-creation", event.get("context"));
        assertNull(event.get("reason"));
        assertFalse(event.toString().contains(secret));
        assertFalse(event.toString().contains(hash));
        assertFalse(created.toString().contains(hash));
        var ordinary = authenticate("SYNTHETIC.ORDINARY", secret);
        assertEquals(created.publicId(), ((AccountPrincipal)ordinary.getPrincipal()).publicId());
        assertNull(ordinary.getCredentials());
        assertTrue(ordinary.getAuthorities().isEmpty());
        SecurityContextHolder.getContext().setAuthentication(ordinary);
        assertEquals(created.publicId(), actor.requireUserId());
        assertThrows(AccessDeniedException.class, () -> actor.requireAuthority("identity:admin"));
        assertDenied(AccessDeniedException.class);
        SecurityContextHolder.getContext().setAuthentication(administrator);
        assertEquals(creator, actor.requireUserId());
        actor.requireAuthority("identity:admin");
        flyway.validate();
        assertEquals(8, count("flyway_schema_history"));
    }

    @Test
    void realSecurityDeniesAbsentAnonymousUnsupportedAndUnauthenticatedBeforeWork() {
        SecurityContextHolder.clearContext(); assertDenied(AuthenticationCredentialsNotFoundException.class);
        var authorities = List.of(new SimpleGrantedAuthority("identity:admin"));
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("synthetic", "anonymousUser", authorities));
        assertDenied(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("unsupported", null, authorities));
        assertDenied(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated(administrator.getPrincipal(), null));
        assertDenied(AuthenticationCredentialsNotFoundException.class);
        verify(encoder, never()).encode(any());
        verifyNoInteractions(audit);
        assertIdentityCounts(1, 1);
    }

    @Test
    void canonicalDuplicateRollsBackWithoutOverwritingResettingOrElevating() {
        String secret = UUID.randomUUID().toString();
        var first = service.create("synthetic.ordinary", secret.toCharArray(), secret.toCharArray());
        String before = jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login='synthetic.ordinary'", String.class);
        char[] password = UUID.randomUUID().toString().toCharArray(), confirmation = password.clone();
        var failure = assertThrows(AccountCreationException.class,
                () -> service.create(" SYNTHETIC.ORDINARY ", password, confirmation));
        assertEquals(AccountCreationException.Reason.DUPLICATE_LOGIN, failure.reason());
        assertNull(failure.getCause()); cleared(password, confirmation);
        assertIdentityCounts(2, 2);
        assertTrue(before.equals(jdbc.queryForObject("SELECT password_hash FROM identity_account WHERE canonical_login='synthetic.ordinary'", String.class)));
        var authenticated = authenticate("synthetic.ordinary", secret);
        assertEquals(first.publicId(), ((AccountPrincipal)authenticated.getPrincipal()).publicId());
        assertTrue(authenticated.getAuthorities().isEmpty());
    }

    @Test
    void auditInsertThenFailureRollsBackAccountAndEventAndClearsBuffers() {
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("Synthetic audit failure");
        }).when(audit).record(any());
        char[] password = UUID.randomUUID().toString().toCharArray(), confirmation = password.clone();
        var failure = assertThrows(AccountCreationException.class,
                () -> service.create("synthetic.rollback", password, confirmation));
        assertEquals(AccountCreationException.Reason.PERSISTENCE_FAILED, failure.reason());
        assertNull(failure.getCause()); cleared(password, confirmation);
        assertIdentityCounts(1, 1);
        reset(audit);
        String secret = UUID.randomUUID().toString();
        service.create("synthetic.rollback", secret.toCharArray(), secret.toCharArray());
        assertIdentityCounts(2, 2);
    }

    @Test
    void databaseFailureRollsBackWithoutSuccessAudit() {
        jdbc.execute("CREATE TRIGGER synthetic_creation_failure BEFORE INSERT ON identity_account FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic creation failure'");
        try {
            char[] password = UUID.randomUUID().toString().toCharArray(), confirmation = password.clone();
            var failure = assertThrows(AccountCreationException.class,
                    () -> service.create("synthetic.failure", password, confirmation));
            assertEquals(AccountCreationException.Reason.PERSISTENCE_FAILED, failure.reason());
            cleared(password, confirmation);
            verifyNoInteractions(audit);
            assertIdentityCounts(1, 1);
        } finally { jdbc.execute("DROP TRIGGER synthetic_creation_failure"); }
    }

    @Test
    void concurrentCanonicalDuplicatesActuallyContendAndCommitOneAccountEventPair() throws Exception {
        CountDownLatch winnerInserted = new CountDownLatch(1), releaseWinner = new CountDownLatch(1);
        doAnswer(call -> {
            winnerInserted.countDown();
            if (!releaseWinner.await(15, TimeUnit.SECONDS)) { throw new IllegalStateException("Synchronization timeout"); }
            return call.callRealMethod();
        }).when(audit).record(any());
        JdbcTemplate observer = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), "root", mysql.getPassword()));
        try (var workers = Executors.newFixedThreadPool(2)) {
            var winner = workers.submit(() -> attempt("synthetic.concurrent"));
            assertTrue(winnerInserted.await(15, TimeUnit.SECONDS));
            var loser = workers.submit(() -> attempt(" SYNTHETIC.CONCURRENT "));
            try {
                boolean waiting = false;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (!waiting && System.nanoTime() < deadline) {
                    waiting = observer.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON w.REQUESTING_ENGINE_LOCK_ID=l.ENGINE_LOCK_ID AND w.ENGINE=l.ENGINE WHERE l.OBJECT_NAME='identity_account'", Integer.class) > 0;
                    Thread.yield();
                }
                assertTrue(waiting, "Competing insert must be observed waiting on MySQL uniqueness lock");
            } finally { releaseWinner.countDown(); }
            assertTrue(winner.get(15, TimeUnit.SECONDS));
            assertFalse(loser.get(15, TimeUnit.SECONDS));
        } finally { releaseWinner.countDown(); }
        assertIdentityCounts(2, 2);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM identity_account WHERE canonical_login='synthetic.concurrent'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='IDENTITY_ACCOUNT_CREATED'", Integer.class));
        verify(audit, times(1)).record(any());
    }

    @Test
    void ambientTransactionFailsClosedBeforeHashingOrMutation() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            char[] password = UUID.randomUUID().toString().toCharArray(), confirmation = password.clone();
            assertThrows(AccountCreationException.class, () -> service.create("synthetic.nested", password, confirmation));
            cleared(password, confirmation);
        });
        verify(encoder, never()).encode(any()); verifyNoInteractions(audit);
        assertIdentityCounts(1, 1);
    }

    private boolean attempt(String login) {
        SecurityContextHolder.getContext().setAuthentication(administrator);
        char[] password = UUID.randomUUID().toString().toCharArray(), confirmation = password.clone();
        try { service.create(login, password, confirmation); return true; }
        catch (AccountCreationException failure) {
            assertEquals(AccountCreationException.Reason.DUPLICATE_LOGIN, failure.reason()); return false;
        } finally { cleared(password, confirmation); SecurityContextHolder.clearContext(); }
    }

    private void assertDenied(Class<? extends RuntimeException> type) {
        char[] password = UUID.randomUUID().toString().toCharArray(), confirmation = password.clone();
        assertThrows(type, () -> service.create("synthetic.denied", password, confirmation));
        cleared(password, confirmation);
    }
    private Authentication authenticate(String login, String secret) {
        return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(login, secret));
    }
    private void assertIdentityCounts(int accounts, int events) {
        assertEquals(accounts, count("identity_account")); assertEquals(events, count("audit_event"));
        for (String table : List.of("identity_role", "identity_permission", "identity_account_role", "identity_role_permission")) { assertEquals(1, count(table)); }
        assertEquals(1, jdbc.queryForObject("SELECT completed FROM identity_bootstrap_state", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE action='FIRST_ADMINISTRATOR_PROVISIONED'", Integer.class));
    }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private static byte[] bytes(UUID id) { return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array(); }
    private static void cleared(char[]... buffers) {
        for (char[] buffer : buffers) {
            boolean empty = true; for (char value : buffer) { empty &= value == 0; }
            assertTrue(empty, "Owned secret buffer must be cleared");
        }
    }
}

package io.github.vncntz.hris;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.github.vncntz.hris.app.LocalProvisioningApplication;
import io.github.vncntz.hris.identityaccess.AccountAuthenticationProvider;
import io.github.vncntz.hris.identityaccess.AccountAuthenticationService;
import io.github.vncntz.hris.identityaccess.AccountPrincipal;
import io.github.vncntz.hris.identityaccess.CurrentActor;
import io.github.vncntz.hris.identityaccess.FirstAdministratorProvisioner;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class FirstAdministratorProvisioningIT {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    private ConfigurableApplicationContext context;
    private JdbcTemplate jdbc;

    @BeforeEach
    void pristineDatabase() {
        Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .cleanDisabled(false).load().clean();
        context = open();
        jdbc = context.getBean(JdbcTemplate.class);
    }

    @AfterEach
    void close() {
        SecurityContextHolder.clearContext();
        if (context != null) { context.close(); }
    }

    @Test
    void nonWebPristineMigrationAndSuccessfulProvisioningAuthenticateNormally() {
        assertFalse(context instanceof WebServerApplicationContext);
        assertFalse(context.containsBean("securityFilterChain"));
        assertEquals(0, context.getBeanNamesForType(com.vaadin.flow.component.UI.class).length);
        assertEmptyIdentity();
        assertEquals(0, count("audit_event"));
        assertEquals(0, completed());
        assertEquals(6, count("flyway_schema_history"));
        String password = UUID.randomUUID().toString();
        UUID id = provisioner().provision(" SYNTHETIC.LOGIN ", password.toCharArray(), password.toCharArray());
        for (String table : identityTables()) { assertEquals(1, count(table)); }
        assertEquals(1, completed());
        assertEquals("administrator", jdbc.queryForObject("SELECT canonical_name FROM identity_role", String.class));
        assertEquals("identity:admin", jdbc.queryForObject("SELECT authority_key FROM identity_permission", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT enabled FROM identity_account", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT enabled FROM identity_role", Integer.class));
        assertTrue(jdbc.queryForObject("SELECT password_hash FROM identity_account", String.class)
                .startsWith("{argon2@SpringSecurity_v5_8}$argon2id$"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        var authentication = context.getBean(AccountAuthenticationProvider.class).authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("SYNTHETIC.LOGIN", password));
        assertEquals(id, assertInstanceOf(AccountPrincipal.class, authentication.getPrincipal()).publicId());
        assertNull(authentication.getCredentials());
        assertEquals(List.of("identity:admin"), authentication.getAuthorities().stream()
                .map(value -> value.getAuthority()).toList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        context.getBean(CurrentActor.class).requireAuthority("identity:admin");
        assertEquals(id, context.getBean(CurrentActor.class).requireUserId());
        assertEquals(1, count("audit_event"));
        assertEquals(id.toString(), jdbc.queryForObject("SELECT actor_reference FROM audit_event", String.class));
        assertEquals(id.toString(), jdbc.queryForObject("SELECT target_reference FROM audit_event", String.class));
        assertEquals("FIRST_ADMINISTRATOR_PROVISIONED", jdbc.queryForObject("SELECT action FROM audit_event", String.class));
        assertEquals("local-operator-bootstrap", jdbc.queryForObject("SELECT context FROM audit_event", String.class));
        assertNull(jdbc.queryForObject("SELECT reason FROM audit_event", String.class));
        assertRefused();
        assertEquals(1, count("audit_event"));
    }

    @Test
    void completionSurvivesRestartDisablementMembershipRemovalAndDeletion() {
        provision();
        context.close();
        context = open();
        jdbc = context.getBean(JdbcTemplate.class);
        assertRefused();
        jdbc.update("UPDATE identity_account SET enabled=0");
        assertRefused();
        jdbc.update("UPDATE identity_role SET enabled=0");
        assertRefused();
        jdbc.update("DELETE FROM identity_account_role");
        jdbc.update("DELETE FROM identity_role_permission");
        assertRefused();
        jdbc.update("DELETE FROM identity_account");
        jdbc.update("DELETE FROM identity_role");
        jdbc.update("DELETE FROM identity_permission");
        assertEmptyIdentity();
        assertRefused();
        assertThrows(RuntimeException.class, () -> jdbc.update("UPDATE identity_bootstrap_state SET completed=0"));
        assertThrows(RuntimeException.class, () -> jdbc.update("DELETE FROM identity_bootstrap_state"));
        assertThrows(RuntimeException.class, () -> jdbc.update("INSERT INTO identity_bootstrap_state VALUES (2,0)"));
        assertEquals(1, completed());
        assertEquals(1, count("audit_event"));
    }

    @Test
    void preexistingRoleOrPermissionFailsClosedWithoutAdoption() {
        jdbc.update("INSERT INTO identity_permission(authority_key) VALUES ('identity:admin')");
        assertRefused();
        assertEquals(0, count("identity_account"));
        assertEquals(0, count("identity_role"));
        assertEquals(0, completed());
        jdbc.update("DELETE FROM identity_permission");
        jdbc.update("INSERT INTO identity_role(public_id,canonical_name,enabled) VALUES (?, 'administrator',1)",
                uuidBytes(UUID.randomUUID()));
        assertRefused();
        assertEquals(0, count("identity_account"));
        assertEquals(0, count("identity_permission"));
        assertEquals(0, count("audit_event"));
    }

    @Test
    void v5UpgradePreservesExistingAccountAndAuthorizationWithoutElevation() {
        context.close();
        Flyway prior = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .cleanDisabled(false).target("5").load();
        prior.clean();
        assertEquals(5, prior.migrate().migrationsExecuted);
        JdbcTemplate old = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        UUID id = UUID.randomUUID();
        String hash = org.springframework.security.crypto.argon2.Argon2PasswordEncoder
                .defaultsForSpringSecurity_v5_8().encode(UUID.randomUUID().toString());
        old.update("INSERT INTO identity_account(public_id,canonical_login,password_hash,enabled,failed_attempts,"
                + "credential_updated_at_utc,security_updated_at_utc) VALUES (?, 'synthetic.legacy', ?, 0, 0, UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))",
                uuidBytes(id), hash);
        old.update("INSERT INTO identity_role(public_id,canonical_name,enabled) VALUES (?, 'legacy.role',0)",
                uuidBytes(UUID.randomUUID()));
        old.update("INSERT INTO identity_permission(authority_key) VALUES ('legacy:read')");
        old.update("INSERT INTO identity_account_role SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r");
        old.update("INSERT INTO identity_role_permission SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p");
        context = open();
        jdbc = context.getBean(JdbcTemplate.class);
        assertRefused();
        assertEquals(1, count("identity_account"));
        assertEquals(0, jdbc.queryForObject("SELECT enabled FROM identity_account", Integer.class));
        assertTrue(hash.equals(jdbc.queryForObject("SELECT password_hash FROM identity_account", String.class)));
        assertTrue(java.util.Arrays.equals(uuidBytes(id), jdbc.queryForObject("SELECT public_id FROM identity_account", byte[].class)));
        for (String table : identityTables()) { assertEquals(1, count(table)); }
        assertEquals(0, jdbc.queryForObject("SELECT enabled FROM identity_role", Integer.class));
        assertEquals("legacy.role", jdbc.queryForObject("SELECT canonical_name FROM identity_role", String.class));
        assertEquals("legacy:read", jdbc.queryForObject("SELECT authority_key FROM identity_permission", String.class));
        assertEquals(0, completed());
        assertEquals(0, count("audit_event"));
        context.getBean(Flyway.class).validate();
    }

    @Test
    void auditAppendThenFailureRollsBackEveryIdentityMutationAndCompletion() {
        AuditRecorder real = context.getBean(AuditRecorder.class);
        FirstAdministratorProvisioner failing = service(request -> {
            real.record(request);
            throw new IllegalStateException("Synthetic audit failure");
        });
        String secret = UUID.randomUUID().toString();
        assertThrows(RuntimeException.class, () -> failing.provision("synthetic.login", secret.toCharArray(), secret.toCharArray()));
        assertEmptyIdentity();
        assertEquals(0, count("audit_event"));
        assertEquals(0, completed());
        provision();
        assertEquals(1, completed());
    }

    @Test
    void databaseFailureRollsBackPartialIdentityAndAllowsCleanRetry() {
        jdbc.execute("CREATE TRIGGER synthetic_membership_failure BEFORE INSERT ON identity_account_role "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic membership failure'");
        assertThrows(RuntimeException.class, this::provision);
        assertEmptyIdentity();
        assertEquals(0, count("audit_event"));
        assertEquals(0, completed());
        jdbc.execute("DROP TRIGGER synthetic_membership_failure");
        provision();
        assertEquals(1, completed());
    }

    @Test
    void competingCommandsActuallyWaitOnSingletonAndCommitExactlyOneAdministrator() throws Exception {
        CountDownLatch holdingLock = new CountDownLatch(1);
        CountDownLatch releaseWinner = new CountDownLatch(1);
        AuditRecorder real = context.getBean(AuditRecorder.class);
        FirstAdministratorProvisioner winner = service(request -> {
            holdingLock.countDown();
            try {
                if (!releaseWinner.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Synchronization timeout"); }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Synchronization interrupted");
            }
            return real.record(request);
        });
        JdbcTemplate observer = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), "root", mysql.getPassword()));
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> attempt(winner, "synthetic.first"));
            assertTrue(holdingLock.await(10, TimeUnit.SECONDS));
            var second = workers.submit(() -> attempt(provisioner(), "synthetic.second"));
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
                boolean waiting = false;
                while (!waiting && System.nanoTime() < deadline) {
                    waiting = observer.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
                            + "JOIN performance_schema.data_locks l ON w.REQUESTING_ENGINE_LOCK_ID=l.ENGINE_LOCK_ID "
                            + "AND w.ENGINE=l.ENGINE WHERE l.OBJECT_NAME='identity_bootstrap_state'", Integer.class) > 0;
                    Thread.yield();
                }
                assertTrue(waiting, "Competing transaction must be observed waiting on the database lock");
            } finally { releaseWinner.countDown(); }
            assertTrue(first.get(10, TimeUnit.SECONDS));
            assertFalse(second.get(10, TimeUnit.SECONDS));
        } finally { releaseWinner.countDown(); }
        for (String table : identityTables()) { assertEquals(1, count(table)); }
        assertEquals(1, count("audit_event"));
        assertEquals(1, completed());
    }

    private static boolean attempt(FirstAdministratorProvisioner service, String login) {
        String secret = UUID.randomUUID().toString();
        try { service.provision(login, secret.toCharArray(), secret.toCharArray()); return true; }
        catch (IllegalStateException closed) { return false; }
    }

    private ConfigurableApplicationContext open() {
        return new SpringApplicationBuilder(LocalProvisioningApplication.class, AuthenticationConfiguration.class)
                .web(WebApplicationType.NONE).properties("spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(), "spring.datasource.password=" + mysql.getPassword(),
                        "logging.level.root=OFF", "spring.main.banner-mode=off").run();
    }

    @ComponentScan(basePackageClasses = AccountAuthenticationService.class)
    static class AuthenticationConfiguration { }

    private FirstAdministratorProvisioner service(AuditRecorder audit) {
        return new FirstAdministratorProvisioner(SharedEntityManagerCreator.createSharedEntityManager(
                context.getBean(EntityManagerFactory.class)), context.getBean(PasswordEncoder.class), audit,
                Clock.systemUTC(), context.getBean(PlatformTransactionManager.class));
    }
    private FirstAdministratorProvisioner provisioner() { return context.getBean(FirstAdministratorProvisioner.class); }
    private void provision() {
        String secret = UUID.randomUUID().toString();
        provisioner().provision("synthetic.login", secret.toCharArray(), secret.toCharArray());
    }
    private void assertRefused() { assertThrows(IllegalStateException.class, this::provision); }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private int completed() { return jdbc.queryForObject("SELECT completed FROM identity_bootstrap_state", Integer.class); }
    private void assertEmptyIdentity() { for (String table : identityTables()) { assertEquals(0, count(table)); } }
    private static List<String> identityTables() {
        return List.of("identity_account", "identity_role", "identity_permission", "identity_account_role", "identity_role_permission");
    }
    private static byte[] uuidBytes(UUID value) {
        return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits()).putLong(value.getLeastSignificantBits()).array();
    }
}

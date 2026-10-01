package io.github.vncntz.hris;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.github.vncntz.hris.identityaccess.AccountAuthenticationProvider;
import io.github.vncntz.hris.identityaccess.AccountPrincipal;
import io.github.vncntz.hris.identityaccess.CurrentActor;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "vaadin.productionMode=true")
@TestMethodOrder(OrderAnnotation.class)
class IdentityAuthorizationIT {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AccountAuthenticationProvider provider;

    @Autowired
    private CurrentActor actor;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // Observe the pristine migrated database before any synthetic fixture is inserted.
    @Test
    @Order(1)
    void cleanMigrationSeedsNoAccountRolePermissionOrAssignment() {
        assertTrue(mysql.isRunning());
        for (String table : List.of("identity_account", "identity_role", "identity_permission",
                "identity_account_role", "identity_role_permission")) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '5' AND success = 1", Integer.class));
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertEquals(List.of("account_id", "role_id"), indexColumns("identity_account_role", "PRIMARY"));
        assertEquals(List.of("role_id", "permission_id"), indexColumns("identity_role_permission", "PRIMARY"));
        assertEquals(List.of("role_id"), indexColumns("identity_account_role", "ix_identity_account_role_role"));
        assertEquals(List.of("permission_id"),
                indexColumns("identity_role_permission", "ix_identity_role_permission_permission"));
    }

    @Test
    void persistedAssignmentsPropagateAuthoritiesAndNewLoginsObserveRevocation() {
        String suffix = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString();
        String login = "synthetic." + suffix;
        UUID publicId = UUID.randomUUID();
        long accountId = insertAccount(jdbc, publicId, login, encoder.encode(password));
        Authentication noRole = authenticate(login, password);
        assertTrue(noRole.getAuthorities().isEmpty());
        SecurityContextHolder.getContext().setAuthentication(noRole);
        assertFalse(actor.hasAuthority("test:approve." + suffix));
        assertThrows(AccessDeniedException.class, () -> actor.requireAuthority("test:approve." + suffix));

        long first = insertRole(UUID.randomUUID(), "first." + suffix, true);
        assignRole(accountId, first);
        assertTrue(authenticate(login, password).getAuthorities().isEmpty());
        long second = insertRole(UUID.randomUUID(), "second." + suffix, true);
        long disabled = insertRole(UUID.randomUUID(), "disabled." + suffix, false);
        long unassigned = insertRole(UUID.randomUUID(), "unassigned." + suffix, true);
        long approve = insertPermission("test:approve." + suffix);
        long view = insertPermission("test:view." + suffix);
        long denied = insertPermission("test:denied." + suffix);
        assignRole(accountId, second);
        assignRole(accountId, disabled);
        assignPermission(first, approve);
        assignPermission(first, view);
        assignPermission(second, approve);
        assignPermission(disabled, denied);
        assignPermission(unassigned, denied);

        Authentication original = authenticate(login.toUpperCase(java.util.Locale.ROOT), password);
        assertEquals(List.of("test:approve." + suffix, "test:view." + suffix), keys(original));
        assertNull(original.getCredentials());
        assertEquals(publicId, assertInstanceOf(AccountPrincipal.class, original.getPrincipal()).publicId());
        SecurityContextHolder.getContext().setAuthentication(original);
        actor.requireAuthority("test:approve." + suffix);
        assertEquals(publicId, actor.requireUserId());
        assertFalse(actor.hasAuthority("test:denied." + suffix));
        assertThrows(AccessDeniedException.class, () -> actor.requireAuthority("test:denied." + suffix));

        jdbc.update("UPDATE identity_role SET enabled = 0 WHERE id = ?", first);
        assertEquals(List.of("test:approve." + suffix), keys(authenticate(login, password)));
        jdbc.update("DELETE FROM identity_role_permission WHERE role_id = ? AND permission_id = ?", second, approve);
        assertTrue(authenticate(login, password).getAuthorities().isEmpty());
        // Existing session keeps its snapshot; future administration must handle explicit revocation.
        actor.requireAuthority("test:view." + suffix);

        jdbc.update("UPDATE identity_role SET enabled = 1 WHERE id = ?", first);
        jdbc.update("DELETE FROM identity_account_role WHERE account_id = ? AND role_id = ?", accountId, first);
        Authentication fresh = authenticate(login, password);
        assertTrue(fresh.getAuthorities().isEmpty());
        SecurityContextHolder.getContext().setAuthentication(fresh);
        assertThrows(AccessDeniedException.class, () -> actor.requireAuthority("test:view." + suffix));
    }

    @Test
    void uniqueCanonicalIdentitiesMembershipsAndForeignKeysAreEnforced() {
        String suffix = UUID.randomUUID().toString();
        UUID publicId = UUID.randomUUID();
        String name = "constraint." + suffix;
        long role = insertRole(publicId, name, true);
        long permission = insertPermission("test:constraint." + suffix);
        long account = insertAccount(jdbc, UUID.randomUUID(), "constraint." + suffix,
                encoder.encode(UUID.randomUUID().toString()));
        assertSqlError(1062, () -> insertRole(publicId, "different." + suffix, true));
        assertSqlError(1062, () -> insertRole(UUID.randomUUID(), name, true));
        for (String invalid : List.of(name.toUpperCase(java.util.Locale.ROOT), "", "bad name", "bad\n", ".bad")) {
            assertSqlError(3819, () -> insertRole(UUID.randomUUID(), invalid, true));
        }
        assertSqlError(3819, () -> jdbc.update("UPDATE identity_role SET enabled = 2 WHERE id = ?", role));
        assertSqlError(1062, () -> insertPermission("test:constraint." + suffix));
        for (String invalid : List.of("TEST:UPPER", "", "bad key", "bad\n", "1bad")) {
            assertSqlError(3819, () -> insertPermission(invalid));
        }

        assignRole(account, role);
        assignPermission(role, permission);
        assertSqlError(1062, () -> assignRole(account, role));
        assertSqlError(1062, () -> assignPermission(role, permission));
        assertSqlError(1452, () -> assignRole(Long.MAX_VALUE, role));
        assertSqlError(1452, () -> assignRole(account, Long.MAX_VALUE));
        assertSqlError(1452, () -> assignPermission(Long.MAX_VALUE, permission));
        assertSqlError(1452, () -> assignPermission(role, Long.MAX_VALUE));
        assertSqlError(1451, () -> jdbc.update("DELETE FROM identity_account WHERE id = ?", account));
        assertSqlError(1451, () -> jdbc.update("DELETE FROM identity_role WHERE id = ?", role));
        assertSqlError(1451, () -> jdbc.update("DELETE FROM identity_permission WHERE id = ?", permission));
    }

    @Test
    void v4UpgradePreservesExistingAccountAndAddsEmptyAuthorizationTables() {
        try (MySQLContainer prior = new MySQLContainer("mysql:8.4.11")
                .withCommand("--log-bin-trust-function-creators=1")) {
            prior.start();
            Flyway v4 = Flyway.configure().dataSource(prior.getJdbcUrl(), prior.getUsername(), prior.getPassword())
                    .target("4").load();
            assertEquals(4, v4.migrate().migrationsExecuted);
            JdbcTemplate upgrade = new JdbcTemplate(new DriverManagerDataSource(
                    prior.getJdbcUrl(), prior.getUsername(), prior.getPassword()));
            UUID publicId = UUID.randomUUID();
            String login = "upgrade." + UUID.randomUUID();
            insertAccount(upgrade, publicId, login, encoder.encode(UUID.randomUUID().toString()));
            Flyway current = Flyway.configure()
                    .dataSource(prior.getJdbcUrl(), prior.getUsername(), prior.getPassword()).load();
            assertEquals(1, current.migrate().migrationsExecuted);
            assertEquals(1, upgrade.queryForObject("SELECT COUNT(*) FROM identity_account WHERE public_id = ?",
                    Integer.class, uuidBytes(publicId)));
            for (String table : List.of("identity_role", "identity_permission", "identity_account_role",
                    "identity_role_permission")) {
                assertEquals(0, upgrade.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
            }
            current.validate();
            assertEquals(0, current.migrate().migrationsExecuted);
        }
    }

    private Authentication authenticate(String login, String password) {
        return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(login, password));
    }

    private long insertRole(UUID publicId, String name, boolean enabled) {
        jdbc.update("INSERT INTO identity_role (public_id, canonical_name, enabled) VALUES (?, ?, ?)",
                uuidBytes(publicId), name, enabled);
        return jdbc.queryForObject("SELECT id FROM identity_role WHERE public_id = ?", Long.class, uuidBytes(publicId));
    }

    private long insertPermission(String key) {
        jdbc.update("INSERT INTO identity_permission (authority_key) VALUES (?)", key);
        return jdbc.queryForObject("SELECT id FROM identity_permission WHERE authority_key = ?", Long.class, key);
    }

    private void assignRole(long account, long role) {
        jdbc.update("INSERT INTO identity_account_role (account_id, role_id) VALUES (?, ?)", account, role);
    }

    private void assignPermission(long role, long permission) {
        jdbc.update("INSERT INTO identity_role_permission (role_id, permission_id) VALUES (?, ?)", role, permission);
    }

    private List<String> indexColumns(String table, String index) {
        return jdbc.queryForList("SELECT column_name FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ? ORDER BY seq_in_index",
                String.class, table, index);
    }

    private static long insertAccount(JdbcTemplate target, UUID publicId, String login, String hash) {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        target.update("INSERT INTO identity_account (public_id, canonical_login, password_hash, enabled, "
                        + "failed_attempts, credential_updated_at_utc, security_updated_at_utc) VALUES (?, ?, ?, 1, 0, ?, ?)",
                uuidBytes(publicId), login, hash, timestamp, timestamp);
        return target.queryForObject("SELECT id FROM identity_account WHERE public_id = ?", Long.class, uuidBytes(publicId));
    }

    private static List<String> keys(Authentication authentication) {
        return authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList();
    }

    private static void assertSqlError(int expectedCode, Executable operation) {
        DataAccessException failure = assertThrows(DataAccessException.class, operation);
        assertEquals(expectedCode, assertInstanceOf(SQLException.class, failure.getMostSpecificCause()).getErrorCode());
    }

    private static byte[] uuidBytes(UUID value) {
        return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits()).putLong(value.getLeastSignificantBits()).array();
    }
}

package io.github.vncntz.hris;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class OfflineMfaMigrationIT {
    @Container static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");
    @Test void populatedV8UpgradePreservesExistingStateAndMatchesCleanV9WithoutSeeds() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        var previous = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).target("8").load();
        assertEquals(8, previous.migrate().migrationsExecuted);
        String unusable = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO identity_account(public_id,canonical_login,password_hash,enabled,failed_attempts,"
                + "credential_updated_at_utc,security_updated_at_utc,authentication_generation,row_version) "
                + "VALUES(UUID_TO_BIN(?),'synthetic.upgrade',?,0,3,'2026-01-02 03:04:05.123456','2026-01-02 03:04:05.123456',9,4)", UUID.randomUUID().toString(), unusable);
        String columns = "id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,locked_until_utc,credential_updated_at_utc,security_updated_at_utc,authentication_generation,row_version";
        var before = jdbc.queryForList("SELECT " + columns + " FROM identity_account");
        var current = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).target("9").cleanDisabled(false).load();
        assertEquals(1, current.migrate().migrationsExecuted); current.validate();
        assertTrue(before.equals(jdbc.queryForList("SELECT " + columns + " FROM identity_account")), "Existing columns must survive upgrade");
        assertNull(jdbc.queryForObject("SELECT mfa_secret FROM identity_account", byte[].class));
        assertEquals(-1, jdbc.queryForObject("SELECT mfa_last_step FROM identity_account", Long.class));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("UPDATE identity_account SET mfa_enabled=1"));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("UPDATE identity_account SET mfa_recovery_hashes='invalid'"));
        String upgraded = schema(jdbc); current.clean(); assertEquals(9, current.migrate().migrationsExecuted);
        assertEquals(upgraded, schema(jdbc)); current.validate(); assertEquals(0, current.migrate().migrationsExecuted);
        for (String table : java.util.List.of("identity_account", "identity_role", "identity_permission", "identity_account_role", "identity_role_permission", "audit_event")) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
    }
    String schema(JdbcTemplate jdbc) { return jdbc.queryForMap("SHOW CREATE TABLE identity_account").get("Create Table").toString().replaceAll(" AUTO_INCREMENT=\\d+", ""); }
}

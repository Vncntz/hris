package io.github.vncntz.hris;

import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RoleGenerationMigrationIT {
    @Container static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");

    @Test void v7UpgradePreservesSyntheticIdentityMembershipAndMatchesCleanV8WithoutSeed() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        var previous = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).target("7").load();
        assertEquals(7, previous.migrate().migrationsExecuted);
        String encoding = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(UUID.randomUUID().toString());
        jdbc.update("INSERT INTO identity_account(public_id,canonical_login,password_hash,enabled,failed_attempts,"
                + "credential_updated_at_utc,security_updated_at_utc,authentication_generation,row_version) "
                + "VALUES(UUID_TO_BIN(?),'synthetic.upgrade',?,0,3,'2026-01-02 03:04:05.123456','2026-01-02 03:04:05.123456',9,4)", UUID.randomUUID().toString(), encoding);
        jdbc.update("INSERT INTO identity_role(public_id,canonical_name,enabled,row_version) VALUES(UUID_TO_BIN(?),'synthetic.role',1,4)", UUID.randomUUID().toString());
        jdbc.update("INSERT INTO identity_permission(authority_key) VALUES('test:upgrade')");
        jdbc.update("INSERT INTO identity_account_role SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r");
        jdbc.update("INSERT INTO identity_role_permission SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p");
        List<String> tables = List.of("identity_account", "identity_permission", "identity_account_role", "identity_role_permission");
        var before = tables.stream().map(table -> snapshot(jdbc, table)).toList();
        String roleBefore = jdbc.queryForList("SELECT id,HEX(public_id),canonical_name,enabled,row_version FROM identity_role").toString();
        var current = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()).cleanDisabled(false).load();
        assertEquals(1, current.migrate().migrationsExecuted);
        assertTrue(before.equals(tables.stream().map(table -> snapshot(jdbc, table)).toList()));
        assertTrue(roleBefore.equals(jdbc.queryForList("SELECT id,HEX(public_id),canonical_name,enabled,row_version FROM identity_role").toString()));
        assertEquals(0L, jdbc.queryForObject("SELECT authorization_generation FROM identity_role", Long.class));
        var negative = assertThrows(org.springframework.jdbc.UncategorizedSQLException.class,
                () -> jdbc.update("UPDATE identity_role SET authorization_generation=-1"));
        assertEquals(3819, negative.getSQLException().getErrorCode());
        assertEquals(0L, jdbc.queryForObject("SELECT authorization_generation FROM identity_role", Long.class));
        current.validate(); assertEquals(0, current.migrate().migrationsExecuted);
        String upgraded = jdbc.queryForMap("SHOW CREATE TABLE identity_role").get("Create Table").toString().replaceAll(" AUTO_INCREMENT=\\d+", "");
        current.clean(); assertEquals(8, current.migrate().migrationsExecuted); current.validate();
        String clean = jdbc.queryForMap("SHOW CREATE TABLE identity_role").get("Create Table").toString().replaceAll(" AUTO_INCREMENT=\\d+", "");
        assertEquals(upgraded, clean);
        for (String table : List.of("identity_account", "identity_role", "identity_permission", "identity_account_role", "identity_role_permission", "audit_event")) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
        assertEquals(8, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1", Integer.class));
    }
    private static String snapshot(JdbcTemplate jdbc, String table) {
        String columns = table.equals("identity_account")
                ? "id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,locked_until_utc,credential_updated_at_utc,security_updated_at_utc,authentication_generation,row_version"
                : "*";
        return jdbc.queryForList("SELECT " + columns + " FROM " + table).toString();
    }

}

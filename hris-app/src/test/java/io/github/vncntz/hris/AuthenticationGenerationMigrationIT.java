package io.github.vncntz.hris;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
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
class AuthenticationGenerationMigrationIT {
    @Container static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");

    @Test void v6UpgradePreservesEveryExistingColumnAndMatchesCleanV7SchemaWithoutSecuritySeed() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        Flyway previous = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .target("6").load();
        assertEquals(6, previous.migrate().migrationsExecuted);
        UUID id = UUID.randomUUID();
        byte[] bytes = ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
        String encoding = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(UUID.randomUUID().toString());
        Timestamp timestamp = Timestamp.from(Instant.parse("2026-01-02T03:04:05.123456Z"));
        jdbc.update("INSERT INTO identity_account (public_id,canonical_login,password_hash,enabled,failed_attempts,"
                + "locked_until_utc,credential_updated_at_utc,security_updated_at_utc,row_version) VALUES (?, ?, ?, 0, 3, ?, ?, ?, 4)",
                bytes, "synthetic.upgrade", encoding, timestamp, timestamp, timestamp);
        String columns = "id,HEX(public_id),canonical_login,password_hash,enabled,failed_attempts,locked_until_utc,"
                + "credential_updated_at_utc,security_updated_at_utc,row_version";
        String before = jdbc.queryForMap("SELECT " + columns + " FROM identity_account").toString();
        Flyway current = Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .cleanDisabled(false).load();
        assertEquals(1, current.migrate().migrationsExecuted);
        assertTrue(before.equals(jdbc.queryForMap("SELECT " + columns + " FROM identity_account").toString()));
        assertEquals(0L, jdbc.queryForObject("SELECT authentication_generation FROM identity_account", Long.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM identity_account", Integer.class));
        assertNoAuthorizationSeed(jdbc);
        current.validate();
        assertEquals(0, current.migrate().migrationsExecuted);
        String upgradedSchema = jdbc.queryForMap("SHOW CREATE TABLE identity_account").get("Create Table").toString()
                .replaceAll(" AUTO_INCREMENT=\\d+", "");
        current.clean();
        assertEquals(7, current.migrate().migrationsExecuted);
        current.validate();
        String cleanSchema = jdbc.queryForMap("SHOW CREATE TABLE identity_account").get("Create Table").toString()
                .replaceAll(" AUTO_INCREMENT=\\d+", "");
        assertEquals(upgradedSchema, cleanSchema);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM identity_account", Integer.class));
        assertNoAuthorizationSeed(jdbc);
        assertEquals(7, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1", Integer.class));
    }

    private static void assertNoAuthorizationSeed(JdbcTemplate jdbc) {
        for (String table : List.of("identity_role", "identity_permission", "identity_account_role", "identity_role_permission", "audit_event")) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
    }
}

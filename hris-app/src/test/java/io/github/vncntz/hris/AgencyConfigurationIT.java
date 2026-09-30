package io.github.vncntz.hris;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.time.ZoneId;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import io.github.vncntz.hris.platformoperations.AgencyConfiguration;
import io.github.vncntz.hris.platformoperations.AgencyConfigurationService;
import io.github.vncntz.hris.sharedkernel.BusinessTimeZone;
import io.github.vncntz.hris.sharedkernel.PublicId;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "vaadin.productionMode=true")
@Import(AgencyConfigurationIT.FixedClockConfiguration.class)
class AgencyConfigurationIT {
    private static final Instant FIXED_TIME = Instant.parse("2026-09-30T02:15:40.123456Z");
    private static final PublicId ACTOR = PublicId.of(UUID.fromString("00000000-0000-0000-0000-000000000007"));
    private static final BusinessTimeZone MANILA = BusinessTimeZone.of("Asia/Manila");

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");

    @Autowired
    private AgencyConfigurationService agency;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestClock clock;

    @BeforeEach
    void removeAgencyForIndependentTest() throws Exception {
        clock.set(FIXED_TIME);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM agency_configuration");
        }
    }

    @Test
    void cleanMigrationHasNoSeedAndRevalidationPreservesHistory() throws Exception {
        assertTrue(mysql.isRunning());
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals(0, count(statement, "SELECT COUNT(*) FROM agency_configuration"));
            assertEquals(1, count(statement,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '4' AND success = 1"));
        }
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    @Test
    void agencySurvivesASecondApplicationContextStartup() {
        AgencyConfiguration created = agency.initialize("Restarted Agency", MANILA, ACTOR);
        try (ConfigurableApplicationContext restarted = new SpringApplicationBuilder(HrisApplication.class)
                .properties("spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(),
                        "spring.datasource.password=" + mysql.getPassword(),
                        "server.port=0", "vaadin.productionMode=true")
                .run()) {
            AgencyConfiguration readBack = restarted.getBean(AgencyConfigurationService.class)
                    .current().orElseThrow();
            assertEquals(created, readBack);
            assertEquals(0, restarted.getBean(Flyway.class).migrate().migrationsExecuted);
        }
    }

    @Test
    void initializeRoundTripsIdentityNameZoneUtcTimeAndAudit() throws Exception {
        int before = auditCount();
        AgencyConfiguration created = agency.initialize("  Example Staffing  ", MANILA, ACTOR);
        assertEquals("Example Staffing", created.displayName());
        assertEquals(MANILA, created.businessTimeZone());
        assertEquals(FIXED_TIME, created.createdAt().value());
        assertEquals(FIXED_TIME, created.updatedAt().value());
        assertEquals(0, created.version());
        assertEquals(created, agency.current().orElseThrow());
        assertEquals(before + 1, auditCount());

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+09:00'");
            try (ResultSet row = statement.executeQuery("""
                    SELECT singleton_key, public_id, display_name, business_time_zone,
                           DATE_FORMAT(created_at_utc, '%Y-%m-%d %H:%i:%s.%f'),
                           DATE_FORMAT(updated_at_utc, '%Y-%m-%d %H:%i:%s.%f')
                      FROM agency_configuration
                    """)) {
                assertTrue(row.next());
                assertEquals(1, row.getInt(1));
                assertArrayEquals(uuidBytes(created.publicId().value()), row.getBytes(2));
                assertEquals("Example Staffing", row.getString(3));
                assertEquals("Asia/Manila", row.getString(4));
                assertEquals("2026-09-30 02:15:40.123456", row.getString(5));
                assertEquals("2026-09-30 02:15:40.123456", row.getString(6));
                assertFalse(row.next());
            }
        }
        assertAudit(created.publicId(), "AGENCY_CONFIGURATION_INITIALIZED", ACTOR);
    }

    @Test
    void duplicateAndDatabaseConstraintsProtectSingletonWithoutSuccessAudit() throws Exception {
        AgencyConfiguration original = agency.initialize("First", MANILA, ACTOR);
        int before = auditCount();
        assertTrue(assertThrows(IllegalStateException.class,
                () -> agency.initialize("Second", MANILA, ACTOR)).getMessage()
                .contains("already initialized"));
        assertEquals(original, agency.current().orElseThrow());
        assertEquals(before, auditCount());

        try (Connection connection = dataSource.getConnection()) {
            SQLException invalidKey = assertThrows(SQLException.class,
                    () -> insertAgency(connection, 2, UUID.randomUUID()));
            assertTrue(invalidKey.getMessage().contains("ck_agency_singleton_key"), invalidKey.getMessage());
            assertThrows(SQLException.class, () -> insertAgency(connection, 1, UUID.randomUUID()));
        }
    }

    @Test
    void updatePreservesIdentityAndCreationTimeAndRejectsStaleVersion() throws Exception {
        AgencyConfiguration created = agency.initialize("First", MANILA, ACTOR);
        int before = auditCount();
        BusinessTimeZone otherZone = BusinessTimeZone.of("Asia/Tokyo");
        Instant changedAt = FIXED_TIME.plusSeconds(60);
        clock.set(changedAt);
        AgencyConfiguration updated = agency.update("  Renamed  ", otherZone, created.version(), ACTOR);
        assertEquals(created.publicId(), updated.publicId());
        assertEquals(created.createdAt(), updated.createdAt());
        assertEquals("Renamed", updated.displayName());
        assertEquals(otherZone, updated.businessTimeZone());
        assertEquals(changedAt, updated.updatedAt().value());
        assertEquals(created.version() + 1, updated.version());
        assertEquals(updated, agency.current().orElseThrow());
        assertEquals(before + 1, auditCount());
        assertAudit(updated.publicId(), "AGENCY_CONFIGURATION_UPDATED", ACTOR);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+09:00'");
            try (ResultSet row = statement.executeQuery("""
                    SELECT DATE_FORMAT(created_at_utc, '%Y-%m-%d %H:%i:%s.%f'),
                           DATE_FORMAT(updated_at_utc, '%Y-%m-%d %H:%i:%s.%f')
                      FROM agency_configuration
                    """)) {
                assertTrue(row.next());
                assertEquals("2026-09-30 02:15:40.123456", row.getString(1));
                assertEquals("2026-09-30 02:16:40.123456", row.getString(2));
            }
        }

        assertTrue(assertThrows(IllegalStateException.class,
                () -> agency.update("Stale", MANILA, created.version(), ACTOR)).getMessage()
                .contains("stale"));
        assertEquals(before + 1, auditCount());
        assertEquals(updated, agency.current().orElseThrow());
        assertEquals(updated, agency.update("Renamed", otherZone, updated.version(), ACTOR));
        assertEquals(before + 1, auditCount());
    }

    @Test
    void concurrentInitializationCommitsExactlyOneRootAndOneAudit() throws Exception {
        int before = auditCount();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Object>> results = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                final int attempt = index;
                Callable<Object> command = () -> {
                    start.await();
                    try {
                        return agency.initialize("Agency " + attempt, MANILA, ACTOR);
                    } catch (RuntimeException failure) {
                        return failure;
                    }
                };
                results.add(executor.submit(command));
            }
            start.countDown();
            int successes = 0;
            int failures = 0;
            for (Future<Object> result : results) {
                Object outcome = result.get();
                if (outcome instanceof AgencyConfiguration) {
                    successes++;
                } else if (outcome instanceof RuntimeException failure) {
                    assertTrue(failure.getMessage().contains("already initialized"),
                            failure.getMessage());
                    failures++;
                }
            }
            assertEquals(1, successes);
            assertEquals(1, failures);
        }
        assertTrue(agency.current().isPresent());
        assertEquals(before + 1, auditCount());
    }

    @Test
    void outerRollbackRemovesAgencyAndAuditTogether() throws Exception {
        int before = auditCount();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        AgencyConfiguration rolledBack = transaction.execute(status -> {
            AgencyConfiguration created = agency.initialize("Rollback", MANILA, ACTOR);
            status.setRollbackOnly();
            return created;
        });
        assertNotEquals(null, rolledBack);
        assertTrue(agency.current().isEmpty());
        assertEquals(before, auditCount());
    }

    private int auditCount() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            return count(statement, "SELECT COUNT(*) FROM audit_event WHERE target_type = 'AGENCY_CONFIGURATION'");
        }
    }

    private void assertAudit(PublicId agencyId, String action, PublicId actorId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT actor_reference, target_reference, reason, context
                       FROM audit_event WHERE target_type = 'AGENCY_CONFIGURATION' AND action = ?
                       AND target_reference = ? ORDER BY id DESC LIMIT 1
                     """)) {
            statement.setString(1, action);
            statement.setString(2, agencyId.toString());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(actorId.toString(), result.getString(1));
                assertEquals(agencyId.toString(), result.getString(2));
                assertEquals(null, result.getString(3));
                assertEquals(null, result.getString(4));
            }
        }
    }

    private static void insertAgency(Connection connection, int key, UUID publicId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO agency_configuration (singleton_key, public_id, display_name,
                    business_time_zone, created_at_utc, updated_at_utc, row_version)
                VALUES (?, ?, 'Other', 'Asia/Manila', '2026-09-30 00:00:00', '2026-09-30 00:00:00', 0)
                """)) {
            statement.setInt(1, key);
            statement.setBytes(2, uuidBytes(publicId));
            statement.executeUpdate();
        }
    }

    private static int count(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static byte[] uuidBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits()).array();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        TestClock agencyTestClock() {
            return new TestClock();
        }
    }

    static final class TestClock extends Clock {
        private final AtomicReference<Instant> current = new AtomicReference<>(FIXED_TIME);

        void set(Instant instant) {
            current.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.ofHours(9);
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}

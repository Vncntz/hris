package io.github.vncntz.hris;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import io.github.vncntz.hris.sharedkernel.RecordedAuditEvent;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "vaadin.productionMode=true")
@Import(AuditPersistenceIT.FixedClockConfiguration.class)
class AuditPersistenceIT {
    private static final Instant FIXED_TIME = Instant.parse("2026-09-30T02:15:40.123456Z");

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");

    @Autowired
    private AuditRecorder recorder;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void appendAndReadBackPreservePublicIdUtcTimeAndCallerFields() throws Exception {
        assertTrue(mysql.isRunning());
        AuditRequest request = new AuditRequest("operator:7", "APPROVED", "synthetic-request",
                "request-14", "review complete", "synthetic workflow summary");
        RecordedAuditEvent recorded = recorder.record(request);
        UUID uuid = recorded.eventId().value();
        assertEquals(FIXED_TIME, recorded.occurredAt().value());

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+09:00'");
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT event_id, DATE_FORMAT(occurred_at_utc, '%Y-%m-%d %H:%i:%s.%f'),
                           actor_reference, action, target_type, target_reference, reason, context
                      FROM audit_event WHERE event_id = ?
                    """)) {
                query.setBytes(1, uuidBytes(uuid));
                try (ResultSet result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertArrayEquals(uuidBytes(uuid), result.getBytes(1));
                    assertEquals("2026-09-30 02:15:40.123456", result.getString(2));
                    assertEquals(request.actorReference(), result.getString(3));
                    assertEquals(request.action(), result.getString(4));
                    assertEquals(request.targetType(), result.getString(5));
                    assertEquals(request.targetReference(), result.getString(6));
                    assertEquals(request.reason(), result.getString(7));
                    assertEquals(request.context(), result.getString(8));
                    assertFalse(result.next());
                }
            }
        }

        LocalDateTime mappedTime = entityManager.createQuery("""
                        SELECT e.occurredAtUtc FROM AuditEventEntity e WHERE e.eventId = :eventId
                        """, LocalDateTime.class)
                .setParameter("eventId", uuid)
                .getSingleResult();
        assertEquals(FIXED_TIME, mappedTime.toInstant(ZoneOffset.UTC));
    }

    @Test
    void directUpdateAndDeleteAreRejectedByDatabase() throws Exception {
        RecordedAuditEvent recorded = recorder.record(minimalRequest());
        byte[] eventId = uuidBytes(recorded.eventId().value());
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("45000", assertThrows(SQLException.class, () -> executeMutation(connection,
                    "UPDATE audit_event SET action = 'CHANGED' WHERE event_id = ?", eventId))
                    .getSQLState());
            assertEquals("45000", assertThrows(SQLException.class, () -> executeMutation(connection,
                    "DELETE FROM audit_event WHERE event_id = ?", eventId))
                    .getSQLState());
            assertEquals(1, countByEventId(connection, eventId));
        }
    }

    @Test
    void auditInsertRollsBackWithSurroundingTransaction() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        RecordedAuditEvent recorded = transaction.execute(status -> {
            RecordedAuditEvent event = recorder.record(minimalRequest());
            status.setRollbackOnly();
            return event;
        });
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(0, countByEventId(connection, uuidBytes(recorded.eventId().value())));
        }
    }

    private static AuditRequest minimalRequest() {
        return new AuditRequest("operator:7", "VIEWED", "synthetic-request", "request-14",
                null, null);
    }

    private static byte[] uuidBytes(UUID uuid) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private static void executeMutation(Connection connection, String sql, byte[] eventId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, eventId);
            statement.executeUpdate();
        }
    }

    private static int countByEventId(Connection connection, byte[] eventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM audit_event WHERE event_id = ?")) {
            statement.setBytes(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt(1);
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedAuditClock() {
            return Clock.fixed(FIXED_TIME, ZoneOffset.ofHours(9));
        }
    }
}

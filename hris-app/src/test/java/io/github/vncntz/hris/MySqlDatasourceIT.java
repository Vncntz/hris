package io.github.vncntz.hris;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "vaadin.productionMode=true"
)
class MySqlDatasourceIT {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
            .withCommand("--log-bin-trust-function-creators=1");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void springDatasourceConnectsToDisposableMySql() throws Exception {
        assertTrue(mysql.isRunning());

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT VERSION(), DATABASE(), 1")) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:mysql:"));
            assertTrue(result.next());
            assertTrue(result.getString(1).startsWith("8.4.11"));
            assertEquals(mysql.getDatabaseName(), result.getString(2));
            assertEquals(1, result.getInt(3));
        }
    }

    @Test
    void flywayAppliesOnceAndRejectsChangedHistory() throws Exception {
        assertTrue(mysql.isRunning());
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:mysql:"));
            assertEquals(1, queryInt(statement, "SELECT baseline_version FROM hris_migration_baseline"));
            assertEquals(1, queryInt(statement,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1"));
            assertEquals(1, queryInt(statement,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = 1"));
            assertEquals(2, queryInt(statement, "SELECT COUNT(*) FROM flyway_schema_history"));
            assertEquals("V1__create_technical_baseline_view.sql", queryString(statement,
                    "SELECT script FROM flyway_schema_history WHERE version = '1'"));
            assertEquals("V2__create_audit_event.sql", queryString(statement,
                    "SELECT script FROM flyway_schema_history WHERE version = '2'"));

            int checksum = queryInt(statement,
                    "SELECT checksum FROM flyway_schema_history WHERE version = '1'");
            flyway.validate();
            assertEquals(0, flyway.migrate().migrationsExecuted);
            assertEquals(2, queryInt(statement, "SELECT COUNT(*) FROM flyway_schema_history"));
            assertEquals(checksum, queryInt(statement,
                    "SELECT checksum FROM flyway_schema_history WHERE version = '1'"));

            Flyway conflictingFlyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:invalid-migration")
                    .load();
            FlywayException failure = assertThrows(FlywayException.class, conflictingFlyway::validate);
            assertTrue(failure.getMessage().toLowerCase().contains("checksum"), failure.getMessage());

            FlywayException collision = assertThrows(FlywayException.class, () -> Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration", "classpath:invalid-migration")
                    .load()
                    .validate());
            assertTrue(collision.getMessage().contains("version 1"), collision.getMessage());
        }
    }

    private static int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static String queryString(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}

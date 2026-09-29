package io.github.vncntz.hris;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "vaadin.productionMode=true"
)
class MySqlDatasourceIT {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11");

    @Autowired
    private DataSource dataSource;

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
}

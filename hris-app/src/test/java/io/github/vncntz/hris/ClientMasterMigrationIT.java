package io.github.vncntz.hris;

import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ClientMasterMigrationIT {
    @Container static final MySQLContainer mysql=new MySQLContainer("mysql:8.4.11").withCommand("--log-bin-trust-function-creators=1");
    @Test void populatedV9UpgradePreservesEveryExistingTableAndMatchesCleanV10WithoutSeeds() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword()));
        var previous=Flyway.configure().dataSource(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword()).target("9").load();
        assertEquals(9,previous.migrate().migrationsExecuted);
        String actor=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO identity_account(public_id,canonical_login,password_hash,enabled,failed_attempts,credential_updated_at_utc,security_updated_at_utc,authentication_generation,row_version) VALUES(UUID_TO_BIN(?),'synthetic.upgrade',?,0,3,'2026-01-02 03:04:05.123456','2026-01-02 03:04:05.123456',9,4)",actor,UUID.randomUUID().toString());
        jdbc.update("INSERT INTO identity_role(public_id,canonical_name,enabled,authorization_generation,row_version) VALUES(UUID_TO_BIN(?),'synthetic.role',1,7,2)",UUID.randomUUID().toString());
        jdbc.update("INSERT INTO identity_permission(authority_key) VALUES('synthetic:read')");
        jdbc.update("INSERT INTO identity_account_role SELECT a.id,r.id FROM identity_account a CROSS JOIN identity_role r");
        jdbc.update("INSERT INTO identity_role_permission SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p");
        jdbc.update("INSERT INTO agency_configuration VALUES(1,UUID_TO_BIN(?),'Synthetic Agency','Asia/Manila','2026-01-02 03:04:05.123456','2026-01-02 03:04:05.123456',3)",UUID.randomUUID().toString());
        jdbc.update("INSERT INTO audit_event(event_id,occurred_at_utc,actor_reference,action,target_type,target_reference) VALUES(UUID_TO_BIN(?),'2026-01-02 03:04:05.123456',?,'SYNTHETIC_UPGRADE','SYNTHETIC',?)",UUID.randomUUID().toString(),actor,actor);
        var existing=List.of("identity_account","identity_role","identity_permission","identity_account_role","identity_role_permission","identity_bootstrap_state","agency_configuration","audit_event");
        var before=new HashMap<String,List<Map<String,Object>>>();
        var schemas=new HashMap<String,String>();
        for (String table:existing) { before.put(table,rows(jdbc,table)); schemas.put(table,schema(jdbc,table)); }
        var checksums=jdbc.queryForList("SELECT version,checksum FROM flyway_schema_history ORDER BY installed_rank");
        var current=Flyway.configure().dataSource(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword()).cleanDisabled(false).load();
        assertEquals(1,current.migrate().migrationsExecuted); current.validate();
        for (String table:existing) { assertEquals(before.get(table),rows(jdbc,table),table); assertEquals(schemas.get(table),schema(jdbc,table),table); }
        assertEquals(checksums,jdbc.queryForList("SELECT version,checksum FROM flyway_schema_history WHERE version <> '10' ORDER BY installed_rank"));
        var upgraded=List.of(schema(jdbc,"client_company"),schema(jdbc,"client_site"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM client_company",Integer.class)); assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM client_site",Integer.class));
        current.clean(); assertEquals(10,current.migrate().migrationsExecuted);
        assertEquals(upgraded,List.of(schema(jdbc,"client_company"),schema(jdbc,"client_site")));
        for (String table:List.of("client_company","client_site","identity_account","identity_role","identity_permission","identity_account_role","identity_role_permission","audit_event","agency_configuration")) {
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
        }
        current.validate(); assertEquals(0,current.migrate().migrationsExecuted);
    }
    List<Map<String,Object>> rows(JdbcTemplate jdbc,String table) {
        var rows=jdbc.queryForList("SELECT * FROM "+table);
        rows.forEach(row -> row.replaceAll((key,value) -> value instanceof byte[] bytes ? HexFormat.of().formatHex(bytes) : value));
        return rows;
    }
    String schema(JdbcTemplate jdbc,String table) { return jdbc.queryForMap("SHOW CREATE TABLE "+table).get("Create Table").toString().replaceAll(" AUTO_INCREMENT=\\d+",""); }
}

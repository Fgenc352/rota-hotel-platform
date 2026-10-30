package com.hotelbooking.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
class LiquibaseMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.6-alpine");

    private Connection database() throws Exception {
        var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        String schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        connection.setSchema(schema);
        return connection;
    }

    private void migrate(Connection connection) throws Exception {
        migrate(connection, "test");
    }

    private void migrate(Connection connection, String context) throws Exception {
        var resources = new ClassLoaderResourceAccessor();
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName(connection.getSchema());
        var liquibase = new Liquibase("db/changelog/db.changelog-master.yml", resources, database);
        // The caller owns the connection and performs assertions after migration.
        liquibase.update(new Contexts(context), new LabelExpression());
        resources.close();
    }

    private void migrateThrough(Connection connection, int changes) throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(connection.getSchema());
            var liquibase = new Liquibase("db/changelog/db.changelog-master.yml", resources, database);
            liquibase.update(changes, new Contexts("test"), new LabelExpression());
        }
    }

    private int count(Connection connection, String query) throws Exception {
        try (var statement = connection.createStatement();
            var rows = statement.executeQuery(query)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    @Test
    void createsFreshSchemaAndDoesNotReseedOnRestart() throws Exception {
        try (var connection = database()) {
            migrate(connection);
            try (var statement = connection.prepareStatement("UPDATE hotel SET name=? WHERE public_key='lagos-beach'")) {
                statement.setString(1, "Persisted hotel name");
                assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            migrate(connection);
            assertThat(count(connection, "SELECT count(*) FROM hotel WHERE name='Persisted hotel name'")).isEqualTo(1);
            assertThat(count(connection, "SELECT count(*) FROM hotel")).isEqualTo(2);
            assertThat(count(connection, "SELECT count(*) FROM room_type")).isEqualTo(5);
            assertThat(count(connection, "SELECT count(*) FROM pms_room")).isEqualTo(50);
            assertThat(count(connection, "SELECT count(*) FROM databasechangelog")).isEqualTo(16);
        }
    }

    @Test
    void demoContextSeedsFiftyGuestsWithConsistentHistoryAndDoesNotDuplicateOnRestart() throws Exception {
        try (var connection = database()) {
            migrate(connection, "demo");
            migrate(connection, "demo");
            assertThat(count(connection, "SELECT count(*) FROM guest")).isEqualTo(50);
            assertThat(count(connection, "SELECT count(*) FROM guest WHERE hotel_id='10000000-0000-0000-0000-000000000001'")).isEqualTo(25);
            assertThat(count(connection, "SELECT count(*) FROM guest WHERE hotel_id='10000000-0000-0000-0000-000000000002'")).isEqualTo(25);
            assertThat(count(connection, "SELECT count(*) FROM guest WHERE matching_conflict")).isEqualTo(2);
            assertThat(count(connection, "SELECT count(*) FROM reservation")).isEqualTo(60);
            assertThat(count(connection, "SELECT count(*) FROM crm_reservation")).isEqualTo(60);
            assertThat(count(connection, "SELECT count(*) FROM reservation_night")).isEqualTo(120);
            assertThat(count(connection, "SELECT sum(sold)::integer FROM room_inventory")).isEqualTo(108);
            assertThat(count(connection, "SELECT count(*) FROM guest_note")).isEqualTo(50);
            assertThat(count(connection, "SELECT count(*) FROM guest_identifier")).isEqualTo(50);
            assertThat(count(connection, "SELECT count(*) FROM outbox_event")).isZero();
            assertThat(count(connection, "SELECT count(*) FROM databasechangelog")).isEqualTo(18);
            assertThat(count(connection, "SELECT count(*) FROM room_type WHERE weekend_multiplier=1.15")).isEqualTo(5);
        }
    }

    @Test
    void addingSnapshotsKeepsOldAuditsWithoutInventingHistory() throws Exception {
        try (var connection = database()) {
            migrateThrough(connection, 8);
            try (var statement = connection.createStatement()) {
                statement.execute("""
                    INSERT INTO guest(id,hotel_id,first_name,last_name,email,phone,consent_at)
                    SELECT ('e1000000-0000-0000-0000-'||lpad(n::text,12,'0'))::uuid,
                        '10000000-0000-0000-0000-000000000001','Old','Guest','old'||n||'@example.com','+90555000000'||n,now()
                    FROM generate_series(1,2) n
                    """);
                statement.execute("""
                    INSERT INTO guest_match_audit(id,hotel_id,source_guest_id,target_guest_id,resolved_by)
                    VALUES ('e2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
                        'e1000000-0000-0000-0000-000000000001','e1000000-0000-0000-0000-000000000002','legacy')
                    """);
            }
            migrate(connection);
            assertThat(count(connection, "SELECT count(*) FROM guest_match_audit WHERE snapshot_version=0 AND before_state IS NULL AND after_state IS NULL")).isEqualTo(1);
        }
    }

    @Test
    void removingTheOptionalCheckoutDayRulePreservesSavedSegmentSettings() throws Exception {
        try (var connection = database()) {
            migrateThrough(connection, 3);
            try (var statement = connection.createStatement()) {
                statement.execute("""
                    INSERT INTO guest_segment(id,hotel_id,name,min_stays,months,days_since_checkout,marketing_required)
                    VALUES ('40000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
                        'Preserved segment',2,12,30,true)
                    """);
            }
            migrate(connection);
            assertThat(count(connection, "SELECT count(*) FROM guest_segment WHERE name='Preserved segment' AND min_stays=2 AND months=12 AND marketing_required"))
                .isEqualTo(1);
            assertThat(count(connection, "SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='guest_segment' AND column_name='days_since_checkout'"))
                .isZero();
        }
    }

    @Test
    void contactMigrationRebuildsPairsWithoutDeletingProfiles() throws Exception {
        try (var connection = database()) {
            migrateThrough(connection, 4);
            String primary = "30000000-0000-0000-0000-000000000001";
            String legacyConflict = "30000000-0000-0000-0000-000000000002";
            String hotel = "10000000-0000-0000-0000-000000000001";
            try (var statement = connection.createStatement()) {
                statement.execute("""
                    INSERT INTO guest(id,hotel_id,first_name,last_name,email,phone,consent_at,matching_conflict)
                    VALUES ('%s','%s','First','Guest','same@example.com','+905551111111',now(),false),
                           ('%s','%s','Legacy','Conflict','other@example.com','+905551111111',now(),true)
                    """.formatted(primary,
                    hotel, legacyConflict, hotel));
                statement.execute("""
                    INSERT INTO guest_identifier(hotel_id,kind,value,guest_id)
                    VALUES ('%s','EMAIL','same@example.com','%s'),
                           ('%s','PHONE','+905551111111','%s'),
                           ('%s','PHONE','+905552222222','%s')
                    """.formatted(hotel,
                    primary, hotel, primary, hotel, primary));
            }
            migrate(connection);
            assertThat(count(connection, "SELECT count(*) FROM guest")).isEqualTo(2);
            assertThat(count(connection, "SELECT count(*) FROM guest_identifier")).isEqualTo(2);
            assertThat(count(connection, "SELECT count(*) FROM guest_identifier WHERE phone='+905552222222'")).isZero();
            assertThat(count(connection, "SELECT count(*) FROM guest_identifier WHERE email='same@example.com' AND phone='+905551111111' AND guest_id='" + primary + "'"))
                .isEqualTo(1);
        }
    }

}

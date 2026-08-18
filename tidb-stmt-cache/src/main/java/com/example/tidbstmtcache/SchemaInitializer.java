package com.example.tidbstmtcache;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code kv} table on app boot. Runs as a CommandLineRunner
 * (not Flyway / Liquibase) deliberately:
 *
 *  - Schema lives in one place that a reviewer can read in 30 seconds.
 *  - The DDL goes through Spring's JdbcTemplate -> Connector/J the same
 *    way as the rest of the workload, so it benefits from keploy's
 *    synthetic-OK fallback for unmocked DDL (matchCommand's
 *    BEGIN/CREATE/DROP/... allowlist in match.go). No mock needs to
 *    exist for replay to satisfy the CREATE TABLE response.
 */
@Component
public class SchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public SchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS kv (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "v INT NOT NULL" +
                ")");

        // Streamed-BLOB fixture for keploy/keploy#4262. Writing this column
        // through PreparedStatement.setBinaryStream is what makes Connector/J
        // pipeline COM_STMT_RESET -> COM_STMT_SEND_LONG_DATA ->
        // COM_STMT_EXECUTE without reading a response in between.
        jdbc.execute("CREATE TABLE IF NOT EXISTS blob_stream (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "payload BLOB NOT NULL" +
                ")");
    }
}

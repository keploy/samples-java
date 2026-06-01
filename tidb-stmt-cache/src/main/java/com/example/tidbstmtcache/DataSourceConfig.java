package com.example.tidbstmtcache;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Single HikariCP pool against TiDB :4000 with MySQL Connector/J flags
 * that force the orphan-EXECUTE scenario this sample is designed around:
 *
 *   useServerPrepStmts=true   -- server-side prepared statements (stmtIDs)
 *   cachePrepStmts=true       -- per-Connection client-side PS cache
 *   prepStmtCacheSize >= 1    -- the cache must actually retain entries
 *
 * Pool sizing > 1 with HikariCP's LIFO eviction means sequential HTTP
 * requests to /api/kv/{i} often land on the same physical connection.
 * On a cache hit, Connector/J skips COM_STMT_PREPARE and emits only
 * COM_STMT_EXECUTE using the cached server-side stmtID. The recorder's
 * mock for that second EXECUTE is the orphan case keploy/keploy@b2e68adb
 * is designed to handle (recordedPrepByConn miss -> expectedQuery="" ->
 * param-alone fallback).
 *
 * TiDB is preferred over MySQL here because TiDB's prepared-statement
 * cache semantics diverge subtly from MySQL across COM_RESET_CONNECTION,
 * which is what surfaced this matcher bug downstream. MySQL 8 alone is
 * unlikely to reproduce the orphan condition reliably in one record cycle.
 */
@Configuration
public class DataSourceConfig {

    @Value("${datasource.tidb.jdbc-url}")
    private String jdbcUrl;

    @Value("${datasource.tidb.username}")
    private String username;

    @Value("${datasource.tidb.password}")
    private String password;

    @Value("${datasource.tidb.driver-class-name}")
    private String driverClass;

    @Bean(destroyMethod = "close")
    public HikariDataSource tidbDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("tidb-dataSource");
        config.setUsername(username);
        config.setPassword(password);
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName(driverClass);

        // Small pool: enough to be realistic (not 1), small enough that
        // sequential curls reliably hit the same physical connection and
        // therefore the same Connector/J prepared-statement cache.
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        // Keep connections alive long enough to span the whole record
        // window so HikariCP doesn't churn the pool mid-test and flush
        // the prep cache out from under us.
        config.setKeepaliveTime(30_000);
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(7_200_000);
        config.setConnectionTimeout(10_000);
        config.setValidationTimeout(5_000);

        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate tidbJdbcTemplate(HikariDataSource tidbDataSource) {
        return new JdbcTemplate(tidbDataSource);
    }
}

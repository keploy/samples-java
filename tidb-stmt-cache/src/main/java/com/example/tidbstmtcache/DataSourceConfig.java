package com.example.tidbstmtcache;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * HikariCP pool against TiDB :4000 with the same bean shape the Flipkart
 * Global-Shipment-Master service uses in production. Two distinct keploy
 * regressions are exercised by this single sample:
 *
 *  - Orphan COM_STMT_EXECUTE (TiDB prepared-statement cache): driven by
 *    useServerPrepStmts + cachePrepStmts + prepStmtCacheSize on the
 *    JDBC URL and HikariCP's LIFO pool — see the existing /api/kv/* and
 *    /api/kv/insert-select/* endpoints.
 *
 *  - Pulsar partitioned-topic SEND round-robin (this file's setAutoCommit
 *    pairing with Hibernate provider_disables_autocommit=true) — driven
 *    by JPA persisting Event rows before publishing to a partitioned
 *    Pulsar topic; the partition the client routes to differs between
 *    record and replay, which is what keploy/enterprise's baseTopic()
 *    matcher loosening addresses.
 *
 * Why autoCommit(false) is wired here and not just left to the driver
 * default: with Hibernate's provider_disables_autocommit=true, the
 * provider expects autocommit to already be off when it acquires a
 * connection. If autocommit defaults to on, Hibernate will issue a
 * redundant SET autocommit=0 on every connection acquisition, which
 * shows up as extra MySQL traffic in the recording and makes the mock
 * stream noisier than it has to be.
 */
@Configuration
public class DataSourceConfig {

    @Value("${keploy.datasource.jdbc-url}")
    private String dbUrl;

    @Value("${keploy.datasource.username}")
    private String dbUsername;

    @Value("${keploy.datasource.password}")
    private String dbPassword;

    @Value("${keploy.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${keploy.datasource.pool-name}")
    private String poolName;

    @Value("${keploy.datasource.minimum-idle}")
    private int minimumIdle;

    @Value("${keploy.datasource.maximum-pool-size}")
    private int maximumPoolSize;

    @Value("${keploy.datasource.idle-timeout}")
    private long idleTimeout;

    @Value("${keploy.datasource.max-lifetime}")
    private long maxLifetime;

    @Value("${keploy.datasource.connection-timeout}")
    private long connectionTimeout;

    @Value("${keploy.datasource.connection-test-query}")
    private String connectionTestQuery;

    @Value("${keploy.datasource.validation-timeout}")
    private long validationTimeout;

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource() {
        String resolvedPassword = dbPassword;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(resolvedPassword);
        config.setDriverClassName(driverClassName);
        config.setPoolName(poolName);
        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);
        config.setConnectionTestQuery(connectionTestQuery);
        config.setValidationTimeout(validationTimeout);
        config.setAutoCommit(false);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(HikariDataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

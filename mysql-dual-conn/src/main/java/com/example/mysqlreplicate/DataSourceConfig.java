package com.example.mysqlreplicate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Replicates the multi-datasource setup that triggers the Keploy
 * "no mysql mocks matched the HandshakeResponse41" error.
 *
 * Two HikariCP pools connect to the SAME MySQL server but with
 * DIFFERENT usernames and databases. During Keploy test replay,
 * each new TCP connection triggers simulateInitialHandshake which
 * must match the client's HandshakeResponse41 against recorded
 * config mocks. The mismatch occurs because:
 *
 * 1. mocks[0] (always omsAppUser/myntra_oms) is used to send the
 *    server greeting to ALL incoming connections.
 * 2. When the camunda pool connects, its HandshakeResponse41 has
 *    username=stagebuster, database=camunda, and different
 *    capability_flags (423535119 vs 20881935).
 * 3. The matcher loops all config mocks looking for a match on
 *    username + database + capability_flags + charset + filler.
 *    If no recorded config mock matches those exact fields, the
 *    error fires.
 */
@Configuration
public class DataSourceConfig {

    // ---- OMS pool (matches omsAppUser / myntra_oms mocks) ----

    @Value("${datasource.oms.jdbc-url}")
    private String omsJdbcUrl;

    @Value("${datasource.oms.username}")
    private String omsUsername;

    @Value("${datasource.oms.password}")
    private String omsPassword;

    @Value("${datasource.oms.driver-class-name}")
    private String omsDriverClass;

    // ---- Camunda pool (matches stagebuster / camunda mocks) ----

    @Value("${datasource.camunda.jdbc-url}")
    private String camundaJdbcUrl;

    @Value("${datasource.camunda.username}")
    private String camundaUsername;

    @Value("${datasource.camunda.password}")
    private String camundaPassword;

    @Value("${datasource.camunda.driver-class-name}")
    private String camundaDriverClass;

    @Bean(name = "omsDataSource", destroyMethod = "close")
    @Primary
    public HikariDataSource omsDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("oms-dataSource");
        config.setUsername(omsUsername);
        config.setPassword(omsPassword);
        return buildDataSource(config, 5, omsJdbcUrl, omsDriverClass);
    }

    @Bean(name = "camundaDataSource", destroyMethod = "close")
    public HikariDataSource camundaDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("camunda-dataSource");
        config.setUsername(camundaUsername);
        config.setPassword(camundaPassword);
        return buildDataSource(config, 5, camundaJdbcUrl, camundaDriverClass);
    }

    @Bean(name = "omsJdbcTemplate")
    @Primary
    public JdbcTemplate omsJdbcTemplate() {
        return new JdbcTemplate(omsDataSource());
    }

    @Bean(name = "camundaJdbcTemplate")
    public JdbcTemplate camundaJdbcTemplate() {
        return new JdbcTemplate(camundaDataSource());
    }

    private HikariDataSource buildDataSource(HikariConfig config, int maxConns,
                                              String jdbcUrl, String driverClass) {
        config.setMaximumPoolSize(maxConns);
        config.setMinimumIdle(2);
        config.setKeepaliveTime(5000);
        config.setIdleTimeout(10000);
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(2000);
        config.setMaxLifetime(7200000);
        config.setLeakDetectionThreshold(2000);
        config.setDriverClassName(driverClass);
        config.setJdbcUrl(jdbcUrl);
        return new HikariDataSource(config);
    }
}

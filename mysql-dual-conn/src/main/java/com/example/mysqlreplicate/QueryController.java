package com.example.mysqlreplicate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple REST controller that queries both datasources.
 * Each endpoint triggers a DB query that forces a connection
 * from the respective pool — reproducing the multi-handshake
 * scenario during Keploy replay.
 */
@RestController
public class QueryController {

    private final JdbcTemplate omsJdbc;
    private final JdbcTemplate camundaJdbc;

    public QueryController(@Qualifier("omsJdbcTemplate") JdbcTemplate omsJdbc,
                           @Qualifier("camundaJdbcTemplate") JdbcTemplate camundaJdbc) {
        this.omsJdbc = omsJdbc;
        this.camundaJdbc = camundaJdbc;
    }

    /**
     * Queries both databases, triggering connections from both pools.
     * During Keploy test mode this forces two distinct HandshakeResponse41
     * packets with different username/database/capability_flags values.
     */
    @GetMapping("/api/query-both")
    public Map<String, Object> queryBoth() {
        Map<String, Object> result = new HashMap<>();

        // OMS query — user=omsAppUser, db=myntra_oms
        List<Map<String, Object>> omsResult = omsJdbc.queryForList("SELECT 1 AS oms_check");
        result.put("oms", omsResult);

        // Camunda query — user=stagebuster, db=camunda
        List<Map<String, Object>> camundaResult = camundaJdbc.queryForList("SELECT 1 AS camunda_check");
        result.put("camunda", camundaResult);

        return result;
    }

    @GetMapping("/api/oms")
    public List<Map<String, Object>> queryOms() {
        return omsJdbc.queryForList("SELECT 1 AS oms_check");
    }

    @GetMapping("/api/camunda")
    public List<Map<String, Object>> queryCamunda() {
        return camundaJdbc.queryForList("SELECT 1 AS camunda_check");
    }

    /**
     * Re-executes a server-prepared statement {@code n} times on the SAME
     * JDBC connection. With useServerPrepStmts=true + useCursorFetch=true,
     * Connector/J 8.x opportunistically emits COM_STMT_RESET before each
     * COM_STMT_EXECUTE after the first, to clear cursor / long-data state.
     *
     * During Keploy replay this exercises the synthetic-OK fallback added
     * in keploy/keploy#4217 — without it, the unmocked COM_STMT_RESET would
     * cascade into "Connection closing due to no matching mock found" and
     * tear down the TCP connection.
     */
    @GetMapping("/api/oms/stmt-reset/{n}")
    public List<Integer> stmtReset(@PathVariable("n") int n) {
        return omsJdbc.execute((java.sql.Connection conn) -> {
            List<Integer> values = new ArrayList<>(n);
            try (PreparedStatement ps = conn.prepareStatement("SELECT ? AS v")) {
                for (int i = 0; i < n; i++) {
                    ps.setInt(1, i);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            values.add(rs.getInt(1));
                        }
                    }
                }
            }
            return values;
        });
    }
}

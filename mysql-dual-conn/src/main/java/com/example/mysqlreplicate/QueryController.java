package com.example.mysqlreplicate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

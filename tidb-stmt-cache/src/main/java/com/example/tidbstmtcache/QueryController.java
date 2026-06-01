package com.example.tidbstmtcache;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints that drive prepared-statement traffic against TiDB :4000.
 *
 * The orphan-EXECUTE scenario being exercised:
 *
 *   1. /api/kv/{v} prepares "SELECT ? AS v" with useServerPrepStmts +
 *      cachePrepStmts on.
 *   2. First call on Connection-A: Connector/J emits COM_STMT_PREPARE
 *      then COM_STMT_EXECUTE; recorder captures both (PREPARE mock +
 *      EXECUTE mock with the SAME connID/stmtID pair).
 *   3. Subsequent call on Connection-A (HikariCP LIFO): Connector/J
 *      finds the PreparedStatement in its client cache and emits ONLY
 *      COM_STMT_EXECUTE using the cached stmtID. Recorder captures an
 *      EXECUTE-only mock.
 *   4. At replay time, the matcher tries to pair the "EXECUTE-only" mock
 *      against the incoming COM_STMT_EXECUTE. If the recorder's connID
 *      attribution or HikariCP's pool rotation makes the PREPARE entry
 *      invisible to buildRecordedPrepIndex for this stmtID, expectedQuery
 *      comes back empty -- which is the case keploy/keploy@b2e68adb
 *      handles by accepting the EXECUTE on parameters alone instead of
 *      crashing the connection with "no matching mock".
 *
 * Two endpoints with different SQL shapes are exposed so the matcher
 * gets nontrivial work to do (it cannot just memoize one stmtID).
 */
@RestController
public class QueryController {

    private final JdbcTemplate jdbc;

    public QueryController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lightweight liveness probe. Plain query, no prepared statement --
     * used by the CI script's wait_for_app loop so app readiness is not
     * coupled to TiDB prep-cache behaviour.
     */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        Map<String, Object> out = new HashMap<>();
        out.put("status", "ok");
        out.put("db", one);
        return out;
    }

    /**
     * Prepared SELECT with one parameter. Same SQL across calls, so
     * Connector/J's cachePrepStmts cache hits on the second-and-later
     * call landing on the same physical connection.
     */
    @GetMapping("/api/kv/{v}")
    public Map<String, Object> selectParam(@PathVariable("v") int v) {
        Integer echoed = jdbc.queryForObject("SELECT ? AS v", Integer.class, v);
        Map<String, Object> out = new HashMap<>();
        out.put("echoed", echoed);
        return out;
    }

    /**
     * Prepared INSERT, then prepared SELECT against the same row. Two
     * distinct prepared statements ("INSERT INTO kv ..." and "SELECT v
     * FROM kv WHERE id=?") that both go through the Connector/J cache.
     * Gives the matcher more than one (connID, stmtID) pair to track
     * concurrently per connection.
     */
    @GetMapping("/api/kv/insert-select/{v}")
    public Map<String, Object> insertThenSelect(@PathVariable("v") int v) {
        jdbc.update("INSERT INTO kv (v) VALUES (?)", v);
        Integer last = jdbc.queryForObject(
                "SELECT v FROM kv ORDER BY id DESC LIMIT 1", Integer.class);
        Map<String, Object> out = new HashMap<>();
        out.put("inserted", v);
        out.put("readback", last);
        return out;
    }
}

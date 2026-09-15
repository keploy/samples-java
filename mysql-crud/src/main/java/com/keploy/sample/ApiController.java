package com.keploy.sample;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ApiController {

    private final JdbcTemplate jdbc;

    public ApiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", (one != null && one == 1) ? "ok" : "degraded");
        return r;
    }

    @GetMapping("/users")
    public Map<String, Object> listUsers() {
        List<Map<String, Object>> users = jdbc.queryForList("SELECT id,name,email FROM users ORDER BY id");
        Integer userCount = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        BigDecimal total = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM orders", BigDecimal.class);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("users", users);
        r.put("userCount", userCount);
        r.put("orderCount", orderCount);
        r.put("totalOrderAmount", total);
        return r;
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(@PathVariable long id) {
        List<Map<String, Object>> u = jdbc.queryForList("SELECT id,name,email FROM users WHERE id=?", id);
        List<Map<String, Object>> orders = jdbc.queryForList(
                "SELECT id,amount,status FROM orders WHERE user_id=? ORDER BY id", id);
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?", Integer.class, id);
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM orders WHERE user_id=?", BigDecimal.class, id);
        jdbc.update("INSERT INTO audit_log(action,detail) VALUES(?,?)", "view_user", "id=" + id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("user", u.isEmpty() ? null : u.get(0));
        r.put("orders", orders);
        r.put("orderCount", cnt);
        r.put("orderTotal", sum);
        return r;
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", "unknown"));
        String email = String.valueOf(body.getOrDefault("email", "unknown@example.com"));

        // LAST_INSERT_ID() is connection-scoped; a pooled JdbcTemplate call can
        // land on a different physical connection than the INSERT. Use
        // generated keys from the same statement/connection instead.
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users(name,email) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, email);
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();

        jdbc.update("INSERT INTO audit_log(action,detail) VALUES(?,?)", "create_user", "name=" + name);
        Integer userCount = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Map<String, Object> created = jdbc.queryForMap("SELECT id,name,email FROM users WHERE id=?", id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("created", created);
        r.put("userCount", userCount);
        return r;
    }

    @PostMapping("/users/{id}/orders")
    public Map<String, Object> createOrder(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Integer userExists = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id=?", Integer.class, id);
        if (userExists == null || userExists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user " + id + " does not exist");
        }

        BigDecimal amount = new BigDecimal(String.valueOf(body.getOrDefault("amount", 0)));
        String status = String.valueOf(body.getOrDefault("status", "PENDING"));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO orders(user_id,amount,status) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, id);
            ps.setBigDecimal(2, amount);
            ps.setString(3, status);
            return ps;
        }, keyHolder);
        long orderId = keyHolder.getKey().longValue();

        Map<String, Object> order = jdbc.queryForMap("SELECT id,user_id,amount,status FROM orders WHERE id=?", orderId);
        Integer orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?", Integer.class, id);
        jdbc.update("INSERT INTO audit_log(action,detail) VALUES(?,?)", "create_order", "user=" + id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("userExists", true);
        r.put("order", order);
        r.put("orderCountForUser", orderCount);
        return r;
    }

    // Deterministic secret-bearing response, used only by Keploy's
    // secret-protection e2e (the enterprise selfhosted-cloud-replay lane).
    // Returns a DETECTED secret in both the body (account.password) and a
    // response header (Token), each next to a BENIGN, NON-superset sibling
    // (account.username; the ordinary X-Request-Id header). Recording this with
    // secret protection enabled makes ObfuscateTestCase run on an HTTP response,
    // so the pipeline can assert the secret was redacted (not persisted in
    // plaintext), the benign sibling survived, and replay still passes.
    //
    // The sibling is deliberately NOT a path-superset of the secret (e.g. NOT
    // password/password_hint): the #2482 fix fails CLOSED and emits no noise for
    // a secret whose path a sibling contains, which would leave the scrambled
    // value un-noised and make replay mismatch. That over-broad case is covered
    // by the unit tests against the real matcher, not here. Values are fixed
    // constants so the CI assertion can grep for them; this endpoint stores
    // nothing and touches no table.
    @GetMapping("/users/{id}/credentials")
    public ResponseEntity<Map<String, Object>> credentials(@PathVariable long id) {
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("username", "benign-sibling-survives");         // benign sibling -> must survive
        account.put("password", "aX7bK9pQ2mZ4rT6vY1nC3hD5fG8jS0lW"); // detected secret -> obfuscated
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("account", account);
        r.put("userId", id);
        return ResponseEntity.ok()
                .header("Token", "aX7bK9pQ2mZ4rT6vY1nC3hD5fG8jS0lW") // detected secret header -> obfuscated
                .header("X-Request-Id", "req-12345")                 // benign header
                .body(r);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer orders = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        BigDecimal sum = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM orders", BigDecimal.class);
        BigDecimal avg = jdbc.queryForObject("SELECT COALESCE(AVG(amount),0) FROM orders", BigDecimal.class);
        BigDecimal max = jdbc.queryForObject("SELECT COALESCE(MAX(amount),0) FROM orders", BigDecimal.class);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("userCount", users);
        r.put("orderCount", orders);
        r.put("sumAmount", sum);
        r.put("avgAmount", avg);
        r.put("maxAmount", max);
        return r;
    }
}

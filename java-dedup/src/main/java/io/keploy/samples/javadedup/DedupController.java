package io.keploy.samples.javadedup;

import io.keploy.samples.javadedup.model.Item;
import io.keploy.samples.javadedup.model.Product;
import io.keploy.samples.javadedup.model.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
public class DedupController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return response("status", "ok");
    }

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {
        return greeting(name);
    }

    @GetMapping("/random")
    public Map<String, Object> random() {
        return response("value", new Random(42).nextInt(2));
    }

    @GetMapping("/welcome")
    public Map<String, Object> welcome(@RequestParam(defaultValue = "Guest") String name) {
        return response("message", "Welcome, " + name);
    }

    @PostMapping("/user")
    public Map<String, Object> createUser(@RequestBody User user) {
        return response("message", "User created successfully", "user", user);
    }

    @GetMapping("/items")
    public List<Item> items() {
        return itemList();
    }

    @PutMapping("/item/{id}")
    public Map<String, Object> updateItem(@PathVariable String id, @RequestBody Item item) {
        return response("message", "Item updated successfully", "id", id, "updatedData", item);
    }

    @DeleteMapping("/item/{id}")
    public Map<String, Object> deleteItem(@PathVariable String id) {
        return response("message", "Item deleted successfully", "id", id);
    }

    @GetMapping({
            "/someone", "/something", "/anyone", "/noone", "/nobody", "/everyone",
            "/anything", "/everything", "/nothing", "/somewhere", "/nowhere",
            "/anybody", "/everybody", "/somebody"
    })
    public Map<String, Object> simpleMessage(HttpServletRequest request) {
        return response("message", messageForPath(request.getRequestURI()));
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return response("service", "user-api", "status", "active");
    }

    @GetMapping("/status/{name}")
    public Map<String, Object> namedStatus(@PathVariable String name) {
        return response("message", greeting(name), "service", "java-dedup");
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        return response("healthy", true);
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return response("version", "1.0.2", "author", "Keploy");
    }

    @GetMapping("/timestamp")
    public Map<String, Object> timestamp() {
        return response("current_time", Instant.now().toString());
    }

    @GetMapping("/products")
    public List<Product> products() {
        return productList();
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Product product) {
        return new ResponseEntity<>(response("status", "product created", "data", product), HttpStatus.CREATED);
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> product(@PathVariable String id) {
        return response("product_id", id, "name", "Sample Product", "price", new BigDecimal("99.99"));
    }

    @PutMapping("/products/{id}")
    public Map<String, Object> updateProduct(@PathVariable String id, @RequestBody Product product) {
        return response("status", "product " + id + " updated", "data", product);
    }

    @DeleteMapping("/products/{id}")
    public Map<String, Object> deleteProduct(@PathVariable String id) {
        return response("status", "product " + id + " deleted");
    }

    @PatchMapping("/products/{id}")
    public Map<String, Object> patchProduct(@PathVariable String id, @RequestBody Map<String, Object> update) {
        return response("status", "product " + id + " partially updated", "patch", update);
    }

    @GetMapping("/users/{userId}/posts/{postId}")
    public Map<String, Object> post(@PathVariable String userId, @PathVariable String postId) {
        return response("user", userId, "post", postId, "content", "This is a sample post.");
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "10") String limit) {
        return response("searching_for", q, "limit", limit);
    }

    @GetMapping("/files/**")
    public Map<String, Object> file(HttpServletRequest request) {
        String prefix = request.getContextPath() + "/files";
        String requestedPath = request.getRequestURI().substring(prefix.length());
        return response("requested_file", requestedPath);
    }

    @GetMapping(value = "/html", produces = MediaType.TEXT_HTML_VALUE)
    public String html() {
        return "<h1>This is HTML</h1>";
    }

    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String xml() {
        return "<user><name>john</name><status>active</status></user>";
    }

    @GetMapping("/redirect")
    public ResponseEntity<Void> redirect() {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("http://google.com"));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    @PatchMapping("/config")
    public Map<String, Object> config(@RequestBody Map<String, Object> update) {
        return response("status", "config updated", "update", update);
    }

    @RequestMapping(value = "/resource", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> resourceOptions() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ALLOW, "GET, POST, OPTIONS");
        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    @GetMapping("/api/v1/data")
    public Map<String, Object> apiV1Data() {
        return response("version", 1, "data", "legacy data");
    }

    @GetMapping("/api/v1/users")
    public Map<String, Object> apiV1Users() {
        return response("version", 1, "users", Arrays.asList("alpha", "beta"));
    }

    @PostMapping("/api/v1/users")
    public ResponseEntity<Map<String, Object>> createApiV1User() {
        return new ResponseEntity<>(response("version", 1, "status", "user created"), HttpStatus.CREATED);
    }

    @GetMapping("/api/v2/data")
    public Map<String, Object> apiV2Data() {
        return response("version", 2, "payload", "new data format");
    }

    @GetMapping("/api/v2/users")
    public Map<String, Object> apiV2Users() {
        Map<String, Object> first = response("name", "gamma");
        Map<String, Object> second = response("name", "delta");
        return response("version", 2, "users", Arrays.asList(first, second));
    }

    @PostMapping("/api/v2/users")
    public ResponseEntity<Map<String, Object>> createApiV2User() {
        return new ResponseEntity<>(response("version", 2, "message", "user successfully registered"), HttpStatus.CREATED);
    }

    @GetMapping("/system/logs")
    public Map<String, Object> logs() {
        return response("log_level", "INFO", "entries", 1024);
    }

    @GetMapping("/system/metrics")
    public Map<String, Object> metrics() {
        return response("cpu_usage", "15%", "memory", "256MB");
    }

    @PostMapping("/system/reboot")
    public ResponseEntity<Map<String, Object>> reboot() {
        return new ResponseEntity<>(response("message", "System reboot initiated"), HttpStatus.ACCEPTED);
    }

    @GetMapping("/proxy")
    public Map<String, Object> proxy() {
        return response("forwarding_to", "downstream-service");
    }

    @GetMapping("/legacy")
    public ResponseEntity<Map<String, Object>> legacy() {
        return new ResponseEntity<>(response("error", "This endpoint is deprecated"), HttpStatus.GONE);
    }

    @GetMapping("/secure/data")
    public ResponseEntity<Map<String, Object>> secureData() {
        return new ResponseEntity<>(response("error", "Authentication required"), HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/admin/panel")
    public ResponseEntity<Map<String, Object>> adminPanel() {
        return new ResponseEntity<>(response("error", "Access denied"), HttpStatus.FORBIDDEN);
    }

    @GetMapping("/long-poll")
    public Map<String, Object> longPoll() throws InterruptedException {
        Thread.sleep(1000L);
        return response("status", "task complete");
    }

    @PutMapping("/user/{id}/password")
    public Map<String, Object> password(@PathVariable String id) {
        return response("message", "password for user " + id + " updated");
    }

    @GetMapping("/user/{id}/profile")
    public Map<String, Object> profile(@PathVariable String id) {
        return response("user_id", id, "profile", "...");
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> events() {
        return new ResponseEntity<>(response("status", "event received"), HttpStatus.ACCEPTED);
    }

    @GetMapping("/session/info")
    public Map<String, Object> sessionInfo() {
        return response("session_id", "xyz-123", "active", true);
    }

    private String greeting(String name) {
        return "Hello, " + name + "!";
    }

    private List<Item> itemList() {
        return Arrays.asList(
                new Item("item1", "Laptop", new BigDecimal("1200.00")),
                new Item("item2", "Mouse", new BigDecimal("25.50")),
                new Item("item3", "Keyboard", new BigDecimal("75.00"))
        );
    }

    private List<Product> productList() {
        return Arrays.asList(
                new Product("prod001", "Eco-friendly Water Bottle", "A reusable bottle.",
                        Arrays.asList("eco", "kitchen")),
                new Product("prod002", "Wireless Charger", "Charges your devices.",
                        Arrays.asList("tech", "mobile"))
        );
    }

    private String messageForPath(String path) {
        String value = path.substring(path.lastIndexOf('/') + 1);
        if ("noone".equals(value)) {
            return "No one";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private Map<String, Object> response(Object... entries) {
        Map<String, Object> response = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            response.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return response;
    }
}

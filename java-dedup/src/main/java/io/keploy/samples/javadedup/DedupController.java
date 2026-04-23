package io.keploy.samples.javadedup;

import io.keploy.samples.javadedup.model.Item;
import io.keploy.samples.javadedup.model.Product;
import io.keploy.samples.javadedup.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DedupController {

    @GetMapping("/")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        return response;
    }

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {
        return greeting(name);
    }

    @PostMapping("/user")
    public Map<String, Object> createUser(@RequestBody User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "User created successfully");
        response.put("user", user);
        return response;
    }

    @PutMapping("/item/{id}")
    public Map<String, Object> updateItem(@PathVariable String id, @RequestBody Item item) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Item updated successfully");
        response.put("id", id);
        response.put("updatedData", item);
        return response;
    }

    @GetMapping("/products")
    public List<Product> products() {
        return Arrays.asList(
                new Product("prod001", "Eco-friendly Water Bottle", "A reusable bottle.",
                        Arrays.asList("eco", "kitchen")),
                new Product("prod002", "Wireless Charger", "Charges your devices.",
                        Arrays.asList("tech", "mobile"))
        );
    }

    @DeleteMapping("/products/{id}")
    public Map<String, Object> deleteProduct(@PathVariable String id) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "product " + id + " deleted");
        return response;
    }

    @GetMapping("/api/v2/users")
    public Map<String, Object> apiUsers() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("name", "gamma");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("name", "delta");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", 2);
        response.put("users", Arrays.asList(first, second));
        return response;
    }

    @GetMapping("/timestamp")
    public Map<String, Object> timestamp() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("current_time", Instant.now().toString());
        return response;
    }

    @GetMapping("/status/{name}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String name) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", greeting(name));
        response.put("service", "java-dedup");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private String greeting(String name) {
        return "Hello, " + name + "!";
    }
}

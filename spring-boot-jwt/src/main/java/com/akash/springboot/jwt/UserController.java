package com.akash.springboot.jwt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private Map<String, String> users = new HashMap<>();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");
        users.put(username, password);

        // TODO: Generate JWT token

        Map<String, String> response = new HashMap<>();
        response.put("token", "demo-token");

        return ResponseEntity.ok(response);
    }

    
    @PostMapping("/tokenAuthentication")
    public ResponseEntity<?> tokenAuthentication(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        //TODO: Validate token
        boolean isValid = true;

        Map<String, Boolean> response = new HashMap<>();
        response.put("isValid", isValid);

        return ResponseEntity.ok(response);
    }
}
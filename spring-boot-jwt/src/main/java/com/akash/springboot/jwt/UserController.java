package com.akash.springboot.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private Map<String, String> users = new HashMap<>();

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");
        users.put(username, password);

        try {
           String token = jwtUtil.generateToken(username);            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            System.err.println(token);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>("Invalid user", HttpStatus.UNAUTHORIZED);
    }

    
    @PostMapping("/tokenAuthentication")
    public ResponseEntity<?> tokenAuthentication(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        //TODO: Validate token
        boolean isValid = jwtUtil.validateToken(token);

        Map<String, Boolean> response = new HashMap<>();
        response.put("isValid", isValid);
        System.err.println(isValid);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
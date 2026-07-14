package com.example.potionsapi.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.example.potionsapi.model.Potion;
import com.example.potionsapi.service.PotionService;

@RestController
public class PotionController {

    @Autowired
    private PotionService potionService;

    @GetMapping("/") // http://localhost:8080
    public ResponseEntity<java.lang.String> homePage() {
        return ResponseEntity.ok().body("<h1> Hello world </h1>");
    }

    @GetMapping("/potions") // http://localhost:8080/potions
    public ResponseEntity<List<Potion>> getAllPotion() {
        return ResponseEntity.ok().body(potionService.getAllPotion());
    }

    @GetMapping("/potions/{id}") // http://localhost:8080/potions/8ab097b9-1a2f-46ab-8825-74313d9eb53c
    public ResponseEntity<Potion> getPotionById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(potionService.getPotionById(id));
    }

    // @GetMapping("/potions/{name}")
    // public ResponseEntity <Potion> getPotionByName( @PathVariable String name ) {
    // return ResponseEntity.ok().body(potionService.getPotionByName(name));
    // }

    @PostMapping("/potions")
    public ResponseEntity<Potion> createPotion(@RequestBody Potion potion) {
        UUID id = UUID.randomUUID();
        potion.setId(id);
        // Make an HTTP client call to a random URL
        try {
            // Create a new book
            createBook("Harry Potter", "J.K. Rowling");

            // Get all books
            getBooks();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // create a new potion
        Potion createdPotion = this.potionService.createPotion(potion);
        return ResponseEntity.ok().body(createdPotion);
    }

    @PutMapping("/potions/{id}") // http://localhost:8080/potions/8ab097b9-1a2f-46ab-8825-74313d9eb53c
    public ResponseEntity<Potion> updatePotion(@PathVariable UUID id, @RequestBody Potion potion) { // removed
                                                                                                    // @ResponseBody //
                                                                                                    // from here
        return ResponseEntity.ok().body(this.potionService.updatePotion(id, potion));
    }

    @DeleteMapping("/potions/{id}") // http://localhost:8080/potions/8ab097b9-1a2f-46ab-8825-74313d9eb53c
    public HttpStatus deletePotion(@PathVariable UUID id) {
        this.potionService.deletePotion(id);
        return HttpStatus.OK;
    }

    private static void createBook(String title, String author) throws IOException {
        URL url = new URL("http://localhost:8085/books");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = "{\"title\": \"" + title + "\", \"author\": \"" + author + "\"}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response: " + response.toString());
        }

        conn.disconnect();
    }

    private static void getBooks() throws IOException {
        URL url = new URL("http://localhost:8085/books");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response: " + response.toString());
        }

        conn.disconnect();

    }
}

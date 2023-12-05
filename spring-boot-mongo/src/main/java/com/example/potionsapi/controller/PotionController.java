package com.example.potionsapi.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.potionsapi.model.Potion;
import com.example.potionsapi.service.PotionService;

@RestController
public class PotionController {

    @Autowired
    private PotionService potionService;

    @GetMapping("/")                // http://localhost:8080
    public ResponseEntity<java.lang.String> homePage() {
        return ResponseEntity.ok().body("<h1> Hello world </h1>");
    }

    @GetMapping("/potions")         // http://localhost:8080/potions
    public ResponseEntity <List <Potion>> getAllPotion() {
        return ResponseEntity.ok().body(potionService.getAllPotion());
    }

    @GetMapping("/potions/{id}")  // http://localhost:8080/potions/8ab097b9-1a2f-46ab-8825-74313d9eb53c
    public ResponseEntity <Potion> getPotionById( @PathVariable UUID id ) {
        return ResponseEntity.ok().body(potionService.getPotionById(id));
    }

//    @GetMapping("/potions/{name}")
//    public ResponseEntity <Potion> getPotionByName( @PathVariable String name ) {
//        return ResponseEntity.ok().body(potionService.getPotionByName(name));
//    }

    @PostMapping("/potions")    // http://localhost:8080/potions
    public ResponseEntity <Potion> createPotion(@RequestBody Potion potion) {
        UUID id = UUID.randomUUID();
        potion.setId(id);
        return ResponseEntity.ok().body(this.potionService.createPotion(potion));
    }

    @PutMapping("/potions/{id}")    // http://localhost:8080/potions/8ab097b9-1a2f-46ab-8825-74313d9eb53c
    public ResponseEntity <Potion> updatePotion(@PathVariable UUID id, @RequestBody Potion potion) {      //removed @ResponseBody from here
        return ResponseEntity.ok().body(this.potionService.updatePotion(id, potion));
    }

    @DeleteMapping("/potions/{id}")    // http://localhost:8080/potions/8ab097b9-1a2f-46ab-8825-74313d9eb53c
    public HttpStatus deletePotion(@PathVariable UUID id) {
        this.potionService.deletePotion(id);
        return HttpStatus.OK;
    }
}
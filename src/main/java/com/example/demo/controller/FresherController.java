package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Fresher;
import com.example.demo.repository.FresherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/")
public class FresherController {

    @Autowired
    private FresherRepository fresherRepository;

    //get fresher
    @GetMapping("freshers")
    public List<Fresher> getAllFreshers() {
        return this.fresherRepository.findAll();
    }

    //get fresher by id
    @GetMapping("freshers/{id}")
    public ResponseEntity<Fresher> getFresherById(@PathVariable(value = "id") Long fresherId) throws ResourceNotFoundException {
        Fresher fresher = fresherRepository.findById(fresherId).orElseThrow(() -> new ResourceNotFoundException("Fresher not found for this id :: " + fresherId));
        return ResponseEntity.ok().body(fresher);
    }

    //save fresher
    @PostMapping("freshers")
    public Fresher createFresher(@RequestBody Fresher fresher) {
        return this.fresherRepository.save(fresher);
    }

    @PostMapping("freshers/all")
    public List<Fresher> saveAllFresher(@RequestBody List<Fresher> fresherList){
        return this.fresherRepository.saveAll(fresherList);
    }

    //update fresher
    @PutMapping("freshers/{id}")
    public ResponseEntity<Fresher> updateFresher(@PathVariable(value = "id") Long fresherId, @Valid @RequestBody Fresher fresherDetails) throws ResourceNotFoundException {

        Fresher fresher = fresherRepository.findById(fresherId).orElseThrow(() -> new ResourceNotFoundException("Fresher not found for this id :: " + fresherId));
        fresher.setFullName(fresherDetails.getFullName());
        fresher.setBatch(fresherDetails.getBatch());

        return ResponseEntity.ok(this.fresherRepository.save(fresher));
    }

    //delete fresher
    @DeleteMapping("freshers/{id}")
    public Map<String, Boolean> deleteFresher(@PathVariable(value = "id") Long fresherId) throws ResourceNotFoundException {
        Fresher fresher = fresherRepository.findById(fresherId).orElseThrow(() -> new ResourceNotFoundException("Fresher not found for this id :: " + fresherId));
        this.fresherRepository.delete(fresher);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);

        return response;
    }
}
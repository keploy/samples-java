package com.example.XML.controllers;

import com.example.XML.entities.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
    @GetMapping(value = "/user", produces = "application/xml")
    public User getUser() {
        return new User("John Doe", 30,"0101233333");
    }
}

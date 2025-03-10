package com.example.XML.controllers;

import com.example.XML.entities.People;
import com.example.XML.entities.User;
import com.example.XML.entities.UserList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping(value = "/user", produces = "application/xml")
    public User getUser() {
        return new User("John Doe", 30, "0101233333");
    }

    @GetMapping(value = "/users", produces = "application/xml")
    public UserList getUsers() {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            users.add(new User("User " + i, 25 + (i % 10), "99999999" + i));
        }
        return new UserList("Main Group", users); // ✅ Fixed
    }

    @GetMapping(value = "/people", produces = "application/xml")
    public People getPeople() {
        List<UserList> userLists = new ArrayList<>();
        
        // Creating multiple UserLists to add to People
        for (int j = 1; j <= 5; j++) { // 5 groups of users
            List<User> users = new ArrayList<>();
            for (int i = 1; i <= 20; i++) { // Each group contains 20 users
                users.add(new User("User " + ((j - 1) * 20 + i), 20 + (i % 10), "98765432" + ((j - 1) * 20 + i)));
            }
            userLists.add(new UserList("Group " + j, users)); // ✅ Fixed
        }

        return new People(userLists);
    }
}

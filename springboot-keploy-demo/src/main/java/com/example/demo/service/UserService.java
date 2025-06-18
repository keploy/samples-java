package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserService {
    private final Map<Long, User> users = new HashMap<>();

    public User createUser(User user) {
        users.put(user.getId(), user);
        return user;
    }

    public User getUserById(Long id) {
        return users.get(id);
    }
}
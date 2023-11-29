package com.example.user.service;

import com.example.user.exception.ResourceNotFoundException;
import com.example.user.model.User;
import com.example.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(Long userId) {
        Optional<User> optional = userRepository.findById(userId);
        if (!optional.isPresent())
            throw new ResourceNotFoundException("User not found for this id :: " + userId);
        return optional.get();
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(Long userId, User user) {
        Assert.isTrue(userId.equals(user.getId()), "Different id input");
        Optional<User> optional = userRepository.findById(userId);
        if (!optional.isPresent())
            throw new ResourceNotFoundException("User not found for this id :: " + userId);
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        Optional<User> optional = userRepository.findById(userId);
        if (!optional.isPresent())
            throw new ResourceNotFoundException("User not found for this id :: " + userId);
        userRepository.delete(optional.get());
    }

}

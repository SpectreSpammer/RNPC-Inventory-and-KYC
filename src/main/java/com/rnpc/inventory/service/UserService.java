package com.rnpc.inventory.service;

import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Authenticate user with username and password
     * @param username the username
     * @param password the password
     * @return Optional containing the user if authentication successful
     */
    public Optional<User> authenticateUser(String username, String password) {
        return userRepository.findByUsernameAndPassword(username, password);
    }

    /**
     * Find user by username
     * @param username the username
     * @return Optional containing the user if found
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Save a new user
     * @param user the user to save
     * @return the saved user
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Check if username exists
     * @param username the username to check
     * @return true if username exists
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Create demo users for testing
     */
    public void createDemoUsers() {
        if (!usernameExists("admin")) {
            User admin = new User("admin", "12345", User.Role.ADMIN);
            saveUser(admin);
        }

        if (!usernameExists("nand159")) {
            User customer = new User("nand159", "12345", User.Role.CUSTOMER);
            saveUser(customer);
        }
    }

    /**
     * Get redirect URL based on user role
     * @param user the authenticated user
     * @return the redirect URL
     */
    public String getRedirectUrl(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return "redirect:http://localhost:9090/admin";
        } else {
            return "redirect:http://localhost:9090/" + user.getUsername();
        }
    }
}
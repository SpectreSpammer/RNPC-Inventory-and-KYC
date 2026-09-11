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
     * Check if email exists
     * @param email the email to check
     * @return true if email exists
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Pre-registers an email as ADMIN so that whenever it signs in with Google, the
     * CustomOAuth2UserService lookup finds this row and reuses ADMIN instead of defaulting a
     * first-time sign-in to CUSTOMER. If the email already has an account, its role is promoted
     * to ADMIN instead of creating a duplicate.
     */
    public void ensureAdminEmail(String email) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(email);
            newUser.setEmail(email);
            newUser.setPassword(null);
            return newUser;
        });
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
    }

    /**
     * Update the profile fields for the currently logged-in user. Username, password, and role
     * are never touched here - this is strictly the account owner editing their own contact
     * details, not an admin-style account management action.
     */
    public User updateProfile(String username, String fullName, String address,
                               String contactNumber, String email) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username: " + username));
        user.setFullName(fullName);
        user.setAddress(address);
        user.setContactNumber(contactNumber);
        user.setEmail(email);
        return userRepository.save(user);
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
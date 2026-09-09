package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email (used for Google sign-in lookups)
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username and password
     * @param username the username
     * @param password the password
     * @return Optional containing the user if found with matching credentials
     */
    Optional<User> findByUsernameAndPassword(String username, String password);

    /**
     * Check if username exists
     * @param username the username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists (used to keep sign-up emails unique)
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);
}
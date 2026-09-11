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
     * Check if email exists (used to keep sign-up emails unique)
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);
}
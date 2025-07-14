package com.rnpc.inventory.config;

import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize demo data on application startup
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        // Create demo users
        userService.createDemoUsers();
        System.out.println("Demo users created:");
        System.out.println("Admin: admin / 12345");
        System.out.println("Customer: nand159 / 12345");
    }
}

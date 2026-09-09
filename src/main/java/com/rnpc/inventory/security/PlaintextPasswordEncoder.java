package com.rnpc.inventory.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Matches this app's existing convention (UserRepository.findByUsernameAndPassword) of storing
 * and comparing credentials as plain text. Not a security best practice, but kept consistent with
 * the rest of the app rather than silently introducing hashing that would break the seeded
 * admin/12345 and nand159/12345 demo accounts.
 */
@Component
public class PlaintextPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encodedPassword != null && rawPassword.toString().equals(encodedPassword);
    }
}

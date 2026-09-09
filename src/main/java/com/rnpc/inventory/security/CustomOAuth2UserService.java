package com.rnpc.inventory.security;

import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Google only ever proves who someone is (a verified email) - it has no concept of "admin" for
 * this app. Role resolution happens entirely against the existing users table: an email already
 * on file keeps whatever role it was given there (including ADMIN, if set up ahead of time);
 * a first-time sign-in is auto-created as CUSTOMER.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Autowired
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account did not return an email address");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(email);
            newUser.setEmail(email);
            newUser.setPassword(null);
            newUser.setRole(User.Role.CUSTOMER);
            // Pre-fill the profile name from the provider's own profile so Edit Profile isn't
            // blank on a brand new account.
            newUser.setFullName(oAuth2User.getAttribute("name"));
            return userRepository.save(newUser);
        });

        Map<String, Object> attributes = oAuth2User.getAttributes();
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "email"
        );
    }
}

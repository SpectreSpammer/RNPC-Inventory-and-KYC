package com.rnpc.inventory.security;

import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Google's OAuth2 registration requests the "openid" scope, which makes it an OIDC provider -
 * Spring Security routes OIDC logins through OidcUserService, NOT the plain OAuth2UserService
 * (see CustomOAuth2UserService, which only ever fires for a non-OIDC provider). Without this
 * class, Google sign-ins were silently handled by Spring's default OidcUserService instead of
 * ours, which assigns a generic OIDC_USER authority - so the email/role lookup below never ran
 * and everyone came back as an implicit non-admin, regardless of what the database said.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Autowired
    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account did not return an email address");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(email);
            newUser.setEmail(email);
            newUser.setPassword(null);
            newUser.setRole(User.Role.CUSTOMER);
            // Pre-fill the profile name from Google's own profile so Edit Profile isn't blank on
            // a brand new account - falls back to given_name if the full name claim is missing.
            String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getGivenName();
            newUser.setFullName(name);
            newUser.setGoogleId(oidcUser.getSubject());
            return userRepository.save(newUser);
        });

        // Backfills the Google subject id for an account that existed before this field was added
        // (e.g. the admin email pre-seeded via ensureAdminEmail) - it becomes that account's
        // employee id (see User.getEmployeeId()) as soon as it signs in with Google at least once.
        if (user.getGoogleId() == null) {
            user.setGoogleId(oidcUser.getSubject());
            user = userRepository.save(user);
        }

        // Explicit nameAttributeKey - the 3-arg constructor defaults to "sub" (Google's opaque
        // subject id), which wouldn't match User.username (set to the email for Google accounts).
        // Using "email" here keeps authentication.getName() consistent across both login methods.
        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }
}

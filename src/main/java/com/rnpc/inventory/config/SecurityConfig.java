package com.rnpc.inventory.config;

import com.rnpc.inventory.security.CustomOAuth2UserService;
import com.rnpc.inventory.security.CustomOidcUserService;
import com.rnpc.inventory.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Login is additive for now: every existing route stays permitAll so nothing currently in the app
 * breaks. This wires up real authentication (username/password against the existing users table,
 * plus Google sign-in) without yet restricting any page to logged-in users - that's a separate,
 * later step once the login page itself works end to end.
 *
 * CSRF is disabled because none of the app's existing forms (product CRUD, orders, repairs,
 * tickets, etc.) carry a CSRF token - enabling it here would 403 every POST/PUT/DELETE across the
 * whole app.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oAuth2UserService;
    private final CustomOidcUserService oidcUserService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                           CustomOAuth2UserService oAuth2UserService,
                           CustomOidcUserService oidcUserService,
                           PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.oAuth2UserService = oAuth2UserService;
        this.oidcUserService = oidcUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                // Google's registration requests "openid", so it authenticates as
                                // an OIDC provider - oidcUserService is the one that actually runs
                                // for it. userService stays wired for any plain OAuth2 (non-OIDC)
                                // provider added later.
                                .userService(oAuth2UserService)
                                .oidcUserService(oidcUserService)
                        )
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}

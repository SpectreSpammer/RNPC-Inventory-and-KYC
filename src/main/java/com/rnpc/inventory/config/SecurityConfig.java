package com.rnpc.inventory.config;

import com.rnpc.inventory.security.CustomOAuth2UserService;
import com.rnpc.inventory.security.CustomOidcUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Locale;

/**
 * Authorization is mostly still handled inside the controllers: every route not listed below stays
 * permitAll at the filter level, and each controller re-checks ROLE_ADMIN itself (see
 * OrderController, AppointmentController, SalesReportController, SupportController). Adding a
 * controller therefore does NOT make it protected.
 *
 * The one exception is the parts inventory. Those eleven controllers had no admin check of their
 * own, which left every route on them - including POST /cpu/create and DELETE /cpu/delete/{id} -
 * open to anonymous users. They are locked here, in the filter chain rather than per method,
 * because the alternative is remembering to guard roughly seventy handler methods individually and
 * every one added later.
 *
 * CSRF is disabled because none of the app's existing forms (product CRUD, orders, repairs,
 * tickets, etc.) carry a CSRF token - enabling it here would 403 every POST/PUT/DELETE across the
 * whole app.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * The parts inventory: PartsController's combined /computer view plus the ten per-category
     * CRUD controllers. The /** entries matter as much as the bare paths - the risk here is the
     * write routes (create, update, removePhoto, delete), not the list pages.
     */
    private static final String[] ADMIN_ONLY_PARTS_PATHS = {
            "/computer", "/computer/**",
            "/cpu/**", "/motherboard/**", "/gpu/**", "/ram/**", "/storage/**",
            "/psu/**", "/case/**", "/cooler/**", "/laptop/**", "/cellphone/**"
    };

    private final CustomOAuth2UserService oAuth2UserService;
    private final CustomOidcUserService oidcUserService;

    @Autowired
    public SecurityConfig(CustomOAuth2UserService oAuth2UserService,
                           CustomOidcUserService oidcUserService) {
        this.oAuth2UserService = oAuth2UserService;
        this.oidcUserService = oidcUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Order matters - the parts rule has to come before the catch-all permitAll.
                // hasRole("ADMIN") matches the ROLE_ADMIN authority CustomOidcUserService already
                // grants; Spring adds the ROLE_ prefix itself, so nothing there needs changing.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ADMIN_ONLY_PARTS_PATHS).hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                // Without this a blocked request renders Spring's 403 error page. Anonymous users
                // get the sign-in page instead, and a signed-in non-admin goes back to their own
                // home page. The two cases need separate handlers: the entry point fires for an
                // AuthenticationException (nobody is logged in), the denied handler for an
                // AccessDeniedException (logged in, wrong role).
                //
                // Both consult the same wantsMachineResponse() predicate rather than one using a
                // DelegatingAuthenticationEntryPoint and the other a lambda: there is no
                // delegating equivalent for the denied side, and if the two paths ever disagreed
                // about what counts as a scripted request, the same fetch() would get a redirect
                // when signed out and a status code when signed in - the exact inconsistency this
                // carve-out exists to avoid.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (wantsMachineResponse(request)) {
                                response.sendError(401);
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login");
                            }
                        })
                        .accessDeniedHandler((request, response, deniedException) -> {
                            if (wantsMachineResponse(request)) {
                                response.sendError(403);
                            } else {
                                response.sendRedirect(request.getContextPath() + "/");
                            }
                        })
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

    /**
     * True when the caller is a script rather than a browser navigation, and so should be told the
     * status code instead of being handed an HTML sign-in page it would silently render into the
     * DOM - or, worse, follow and report as success.
     *
     * Note that a bare fetch(url) sends neither of these: its default Accept is the wildcard, and
     * X-Requested-With is an explicit opt-in. A fetch that wants a status code back has to ask for
     * one, e.g. fetch(url, {headers: {'X-Requested-With': 'XMLHttpRequest'}}).
     */
    private static boolean wantsMachineResponse(HttpServletRequest request) {
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return true;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept == null || accept.isBlank()) {
            return false;
        }
        // "Prefers JSON" means JSON is asked for and HTML is not. A browser navigation sends
        // text/html first, so it never matches even though its Accept list is long.
        String lower = accept.toLowerCase(Locale.ROOT);
        return lower.contains(MediaType.APPLICATION_JSON_VALUE)
                && !lower.contains(MediaType.TEXT_HTML_VALUE);
    }
}

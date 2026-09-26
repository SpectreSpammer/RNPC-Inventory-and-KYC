package com.rnpc.inventory.config;

import com.rnpc.inventory.security.CustomOAuth2UserService;
import com.rnpc.inventory.security.CustomOidcUserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The customer and walk-in ticket routes are admin-only in the filter chain (SecurityConfig
 * .ADMIN_ONLY_CUSTOMER_PATHS), the same way the parts routes are. This runs the real SecurityConfig
 * in a minimal web context - stub controllers, mocked user services, no Spring Boot app and no
 * database - and checks the three cases for a list, a create and a slip route:
 *
 *   anonymous          -> redirected to /login
 *   signed-in non-admin -> redirected to /
 *   admin              -> reaches the controller
 *
 * plus the scripted-request carve-out (X-Requested-With: XMLHttpRequest gets 401 / 403 instead of a
 * redirect), and that /ticket/view - the customer's own Repair History - is NOT locked.
 */
class AdminOnlyCustomerRoutesSecurityTest {

    /** Stands in for ClientController and TicketController; only the mappings matter. */
    @Controller
    static class StubController {
        @GetMapping({"/client", "/client/"})
        @ResponseBody String clientList() { return "ok"; }

        @GetMapping("/client/create")
        @ResponseBody String clientCreateForm() { return "ok"; }

        @PostMapping("/client/create")
        @ResponseBody String clientCreate() { return "ok"; }

        @DeleteMapping("/client/delete/{id}")
        @ResponseBody String clientDelete(@PathVariable Long id) { return "ok"; }

        @GetMapping("/ticket/create")
        @ResponseBody String ticketCreateForm() { return "ok"; }

        @PostMapping("/ticket/create")
        @ResponseBody String ticketCreate() { return "ok"; }

        @GetMapping("/ticket/{id}")
        @ResponseBody String slip(@PathVariable Long id) { return "ok"; }

        @GetMapping("/ticket/{id}/modal")
        @ResponseBody String slipModal(@PathVariable Long id) { return "ok"; }

        @GetMapping("/ticket/view")
        @ResponseBody String view() { return "ok"; }
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean CustomOAuth2UserService oAuth2UserService() { return mock(CustomOAuth2UserService.class); }
        @Bean CustomOidcUserService oidcUserService() { return mock(CustomOidcUserService.class); }
        @Bean StubController stubController() { return new StubController(); }

        // oauth2Login() needs a registration repository to build; nothing here ever contacts it.
        @Bean ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("google")
                    .clientId("test").clientSecret("test")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("https://example.invalid/auth")
                    .tokenUri("https://example.invalid/token")
                    .build());
        }
    }

    private static AnnotationConfigWebApplicationContext context;
    private static MockMvc mvc;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", FilterChainProxy.class))
                .build();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    private static MockHttpSession sessionWithRole(String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken("someone@example.com", "n/a",
                List.of(new SimpleGrantedAuthority(role)));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(auth));
        return session;
    }

    private static MockHttpServletRequestBuilder asCustomer(MockHttpServletRequestBuilder request) {
        return request.session(sessionWithRole("ROLE_CUSTOMER"));
    }

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.session(sessionWithRole("ROLE_ADMIN"));
    }

    private static MockHttpServletRequestBuilder scripted(MockHttpServletRequestBuilder request) {
        return request.header("X-Requested-With", "XMLHttpRequest");
    }

    /** Runs the whole matrix for one route; build() must return a fresh request each call. */
    private void assertAdminOnly(java.util.function.Supplier<MockHttpServletRequestBuilder> build) throws Exception {
        mvc.perform(build.get()).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(asCustomer(build.get())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        mvc.perform(asAdmin(build.get())).andExpect(status().isOk());
        mvc.perform(scripted(build.get())).andExpect(status().isUnauthorized());
        mvc.perform(asCustomer(scripted(build.get()))).andExpect(status().isForbidden());
    }

    @Test
    void clientListIsAdminOnly() throws Exception {
        assertAdminOnly(() -> get("/client"));
        assertAdminOnly(() -> get("/client/"));
    }

    @Test
    void clientWriteRoutesAreAdminOnly() throws Exception {
        assertAdminOnly(() -> get("/client/create"));
        assertAdminOnly(() -> post("/client/create"));
        assertAdminOnly(() -> delete("/client/delete/5"));
    }

    @Test
    void ticketCreateIsAdminOnly() throws Exception {
        assertAdminOnly(() -> get("/ticket/create"));
        assertAdminOnly(() -> post("/ticket/create"));
    }

    @Test
    void ticketSlipAndItsModalAreAdminOnly() throws Exception {
        assertAdminOnly(() -> get("/ticket/7"));
        assertAdminOnly(() -> get("/ticket/7/modal"));
    }

    @Test
    void ticketViewStaysOpenBecauseItIsTheCustomersOwnRepairHistory() throws Exception {
        mvc.perform(get("/ticket/view")).andExpect(status().isOk());
        mvc.perform(asCustomer(get("/ticket/view"))).andExpect(status().isOk());
        mvc.perform(asAdmin(get("/ticket/view"))).andExpect(status().isOk());
    }
}

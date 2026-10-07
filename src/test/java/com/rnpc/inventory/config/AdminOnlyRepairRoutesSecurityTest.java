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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The repair record's write routes are admin-only in the filter chain (SecurityConfig
 * .ADMIN_ONLY_REPAIR_PATHS), while the customer's own reads stay open. Runs the real SecurityConfig
 * in a minimal web context - stub controller, mocked user services, no Spring Boot app and no
 * database - and checks, for every write route:
 *
 *   anonymous           -> redirected to /login
 *   signed-in non-admin -> redirected to /
 *   admin               -> reaches the controller
 *
 * plus the scripted-request carve-out (X-Requested-With gets 401 / 403, not a redirect), and that
 * GET /repair, GET /repair/{id}/modal and /ticket/view are NOT locked, since customers use them.
 */
class AdminOnlyRepairRoutesSecurityTest {

    /** Stands in for RepairRecordController and TicketController; only the mappings matter. */
    @Controller
    static class StubController {
        @GetMapping({"/repair", "/repair/"})
        @ResponseBody String list() { return "ok"; }

        @GetMapping("/repair/{id}/modal")
        @ResponseBody String modal(@PathVariable Long id) { return "ok"; }

        @GetMapping("/repair/create")
        @ResponseBody String createForm() { return "ok"; }

        @PostMapping("/repair/create")
        @ResponseBody String create() { return "ok"; }

        @GetMapping("/repair/edit/{id}")
        @ResponseBody String editForm(@PathVariable Long id) { return "ok"; }

        @PutMapping("/repair/update/{id}")
        @ResponseBody String update(@PathVariable Long id) { return "ok"; }

        @DeleteMapping("/repair/removePhoto/{id}")
        @ResponseBody String removePhoto(@PathVariable Long id) { return "ok"; }

        @DeleteMapping("/repair/delete/{id}")
        @ResponseBody String delete(@PathVariable Long id) { return "ok"; }

        @GetMapping("/ticket/view")
        @ResponseBody String ticketView() { return "ok"; }
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
    void createRoutesAreAdminOnly() throws Exception {
        assertAdminOnly(() -> get("/repair/create"));
        assertAdminOnly(() -> post("/repair/create"));
    }

    @Test
    void editAndUpdateRoutesAreAdminOnly() throws Exception {
        assertAdminOnly(() -> get("/repair/edit/5"));
        assertAdminOnly(() -> put("/repair/update/5"));
    }

    @Test
    void removePhotoAndDeleteRoutesAreAdminOnly() throws Exception {
        assertAdminOnly(() -> delete("/repair/removePhoto/5"));
        assertAdminOnly(() -> delete("/repair/delete/5"));
    }

    @Test
    void theRepairListAndItsModalStayOpenForTheCustomersOwnHistory() throws Exception {
        for (String path : List.of("/repair", "/repair/", "/repair/5/modal", "/ticket/view")) {
            mvc.perform(get(path)).andExpect(status().isOk());
            mvc.perform(asCustomer(get(path))).andExpect(status().isOk());
            mvc.perform(asAdmin(get(path))).andExpect(status().isOk());
        }
    }
}

package com.rnpc.inventory.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Makes the current user's role available to every Thymeleaf-rendered page's sidebar (see
// fragments/nav.html) at render time, so admin-only menu items are correct in the very first
// HTML the browser gets - no client-side fetch, no flash of the wrong menu while it resolves.
// Named navIsAdmin/navAuthenticated (not isAdmin) so this never collides with a controller's own
// same-named model attribute used for its own page content.
@ControllerAdvice
public class GlobalNavAttributes {

    @ModelAttribute("navAuthenticated")
    public boolean navAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("navIsAdmin")
    public boolean navIsAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

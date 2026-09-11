package com.rnpc.inventory.controller;

import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class LoginController {

    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public LoginController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    // POST /login and GET /logout are both handled entirely by Spring Security (see
    // SecurityConfig) - this controller only needs to render the page itself.
    @GetMapping("/login")
    public String showLoginPage(Model model,
                                 @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("error", "Google sign-in failed. Please try again.");
        }
        return "login/login";
    }

    // The landing page (static/index.html) isn't a Thymeleaf template, so it can't check
    // authentication server-side - it calls this instead to decide whether to pop the sign-in
    // modal open on load.
    @GetMapping("/api/auth/status")
    @ResponseBody
    public Map<String, Object> authStatus(Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authenticated", authenticated);
        if (authenticated) {
            body.put("username", authentication.getName());

            // Priority: the name the account owner explicitly set via Edit Profile, then
            // Google's own first name (friendlier than the email used as username), then the
            // raw username as a last resort for local accounts with neither.
            String displayName = authentication.getName();
            if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
                String givenName = oAuth2User.getAttribute("given_name");
                if (givenName != null && !givenName.isBlank()) {
                    displayName = givenName;
                }
            }
            String profileName = userService.findByUsername(authentication.getName())
                    .map(User::getFullName)
                    .orElse(null);
            if (profileName != null && !profileName.isBlank()) {
                displayName = profileName;
            }
            body.put("displayName", displayName);

            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse(null);
            body.put("role", role);

            long unread = "ADMIN".equals(role)
                    ? notificationService.getUnreadCountForAdmin()
                    : notificationService.getUnreadCountForUser(authentication.getName());
            body.put("unreadNotifications", unread);
        }
        return body;
    }
}

package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.ProfileDto;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class ProfileController {

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/edit")
    public String showEditPage(Authentication authentication, Model model,
                                @RequestParam(value = "saved", required = false) String saved) {
        if (!isSignedIn(authentication)) {
            // An idle session that's since expired should never bounce someone to the standalone
            // /login page - land them on the homepage instead, where they can sign back in via
            // the same modal used everywhere else.
            return "redirect:/";
        }
        Optional<User> currentUser = currentUser(authentication);
        if (currentUser.isEmpty()) {
            // A session from before a Google-login identity fix can point at an account that no
            // longer resolves - force a clean logout instead of crashing so a fresh sign-in fixes it.
            return "redirect:/logout";
        }
        User user = currentUser.get();

        // Accounts created before profile names existed (or before this fallback was added)
        // would otherwise see a blank Name field - fall back to the Google profile name (for
        // OAuth/OIDC sessions) or the username, so there's always something sensible to start
        // from, and saving the form persists it into fullName from then on.
        String fullName = user.getFullName();
        if ((fullName == null || fullName.isBlank()) && authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String googleName = oAuth2User.getAttribute("name");
            if (googleName != null && !googleName.isBlank()) {
                fullName = googleName;
            }
        }
        if (fullName == null || fullName.isBlank()) {
            fullName = user.getUsername();
        }

        ProfileDto dto = new ProfileDto();
        dto.setFullName(fullName);
        dto.setAddress(user.getAddress());
        dto.setContactNumber(user.getContactNumber());
        dto.setEmail(user.getEmail());

        model.addAttribute("profileDto", dto);
        // The employee id is just this account's own numeric User.id - there's no separate
        // HR/employee table, and it's what Order.verifiedByEmployeeId records against.
        model.addAttribute("isAdmin", user.getRole() == User.Role.ADMIN);
        model.addAttribute("employeeId", user.getEmployeeId());
        if (saved != null) {
            model.addAttribute("saved", true);
        }
        return "profile/profileEdit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(Authentication authentication,
                                 @Valid @ModelAttribute ProfileDto profileDto,
                                 BindingResult result,
                                 Model model) {
        if (!isSignedIn(authentication)) {
            // An idle session that's since expired should never bounce someone to the standalone
            // /login page - land them on the homepage instead, where they can sign back in via
            // the same modal used everywhere else.
            return "redirect:/";
        }
        Optional<User> currentUser = currentUser(authentication);
        if (currentUser.isEmpty()) {
            return "redirect:/logout";
        }
        User user = currentUser.get();

        // Only flag a conflict if the new email belongs to a DIFFERENT account - otherwise
        // re-saving your own unchanged email would incorrectly look like a duplicate.
        boolean emailTaken = userService.emailExists(profileDto.getEmail())
                && !profileDto.getEmail().equalsIgnoreCase(user.getEmail());
        if (emailTaken) {
            result.rejectValue("email", "duplicate", "That email is already in use by another account.");
        }

        if (result.hasErrors()) {
            return "profile/profileEdit";
        }

        userService.updateProfile(user.getUsername(), profileDto.getFullName(), profileDto.getAddress(),
                profileDto.getContactNumber(), profileDto.getEmail());
        return "redirect:/profile/edit?saved";
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private Optional<User> currentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }
}

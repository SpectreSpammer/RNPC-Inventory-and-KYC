package com.rnpc.inventory.config;

import com.rnpc.inventory.service.ProfilePhotoService;
import com.rnpc.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Makes attributes every layout-app page needs available at render time, regardless of which
// controller is rendering it - no client-side fetch, no flash of the wrong state while it resolves.
//
//  - navAuthenticated / navIsAdmin: the current user's role, for fragments/nav.html's admin-only
//    menu items. Named with the nav prefix (not isAdmin) so this never collides with a controller's
//    own same-named model attribute used for its own page content.
//  - hasProfilePhoto: whether the topbar avatar (layout-app's user-avatar) should show the uploaded
//    photo instead of the username's first letter. Before this, only ProfileController set it (in
//    its own profileModel(...)), so the photo appeared only on the profile page - see CLAUDE.md's
//    former "Known dead code" entry. A controller can still set its own "hasProfilePhoto" if it
//    already has the User loaded (ProfileController does, and its value wins - Spring keeps the
//    last write to a Model key), but nothing else has to.
@ControllerAdvice
public class GlobalNavAttributes {

    private final UserService userService;
    private final ProfilePhotoService profilePhotos;

    @Autowired
    public GlobalNavAttributes(UserService userService, ProfilePhotoService profilePhotos) {
        this.userService = userService;
        this.profilePhotos = profilePhotos;
    }

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

    @ModelAttribute("hasProfilePhoto")
    public boolean hasProfilePhoto(Authentication authentication) {
        if (!navAuthenticated(authentication)) {
            return false;
        }
        return userService.findByUsername(authentication.getName())
                .map(user -> profilePhotos.exists(user.getId()))
                .orElse(false);
    }
}

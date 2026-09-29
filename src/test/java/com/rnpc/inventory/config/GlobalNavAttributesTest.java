package com.rnpc.inventory.config;

import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.ProfilePhotoService;
import com.rnpc.inventory.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * hasProfilePhoto used to be set only by ProfileController's own profileModel(...), so the topbar
 * avatar showed the uploaded photo only on the profile page and fell back to the initial everywhere
 * else. It is now a @ModelAttribute here, alongside navAuthenticated/navIsAdmin, so every page gets
 * it the same way. Services are mocked - no Spring context, no database, no filesystem.
 */
class GlobalNavAttributesTest {

    private final UserService userService = mock(UserService.class);
    private final ProfilePhotoService photos = mock(ProfilePhotoService.class);
    private final GlobalNavAttributes attrs = new GlobalNavAttributes(userService, photos);

    private static User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private static Authentication signedIn(String username) {
        return new UsernamePasswordAuthenticationToken(username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void anAnonymousVisitorNeverHasAPhoto() {
        Authentication anon = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertFalse(attrs.hasProfilePhoto(anon));
        assertFalse(attrs.hasProfilePhoto(null));
    }

    @Test
    void aSignedInUserWithAStoredPhotoGetsTrue() {
        when(userService.findByUsername("nand@example.com")).thenReturn(Optional.of(user(7L)));
        when(photos.exists(7L)).thenReturn(true);

        assertTrue(attrs.hasProfilePhoto(signedIn("nand@example.com")));
    }

    @Test
    void aSignedInUserWithNoStoredPhotoGetsFalse() {
        when(userService.findByUsername("nand@example.com")).thenReturn(Optional.of(user(7L)));
        when(photos.exists(7L)).thenReturn(false);

        assertFalse(attrs.hasProfilePhoto(signedIn("nand@example.com")));
    }

    @Test
    void aUsernameThatDoesNotResolveToAUserGetsFalseRatherThanThrowing() {
        when(userService.findByUsername("ghost@example.com")).thenReturn(Optional.empty());

        assertFalse(attrs.hasProfilePhoto(signedIn("ghost@example.com")));
        org.mockito.Mockito.verify(photos, org.mockito.Mockito.never()).exists(any());
    }

    @Test
    void navAuthenticatedAndNavIsAdminAreUnaffected() {
        Authentication admin = new UsernamePasswordAuthenticationToken("admin@example.com", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(attrs.navAuthenticated(admin));
        assertTrue(attrs.navIsAdmin(admin));

        Authentication customer = signedIn("nand@example.com");
        assertTrue(attrs.navAuthenticated(customer));
        assertFalse(attrs.navIsAdmin(customer));

        assertFalse(attrs.navAuthenticated(null));
        assertFalse(attrs.navIsAdmin(null));
    }
}

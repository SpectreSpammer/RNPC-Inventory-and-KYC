package com.rnpc.inventory.controller;

import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("notification")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping({"", "/"})
    public String showNotifications(Authentication authentication, Model model) {
        boolean admin = isAdmin(authentication);
        List<Notification> notifications;
        if (admin) {
            notifications = notificationService.getForAdmin();
            notificationService.markAllReadForAdmin();
        } else if (isSignedIn(authentication)) {
            notifications = notificationService.getForUser(authentication.getName());
            notificationService.markAllReadForUser(authentication.getName());
        } else {
            notifications = List.of();
        }
        model.addAttribute("notifications", notifications);
        return "notifications/notificationIndex";
    }

    // Called from the topbar dropdown's "Mark all as read" (layout-app.html) via fetch, so the
    // customer/admin never leaves the page they're on - unlike showNotifications above, which only
    // marks read as a side effect of loading the full /notification page. Same
    // markAllReadForAdmin/markAllReadForUser split as showNotifications (NotificationService.java:67,
    // :73), no new business logic.
    @PostMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        if (isAdmin(authentication)) {
            notificationService.markAllReadForAdmin();
        } else if (isSignedIn(authentication)) {
            notificationService.markAllReadForUser(authentication.getName());
        } else {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}

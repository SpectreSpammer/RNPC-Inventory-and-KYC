package com.rnpc.inventory.controller;

import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

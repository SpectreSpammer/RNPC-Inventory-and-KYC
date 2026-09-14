package com.rnpc.inventory.controller;

import com.rnpc.inventory.service.AdminDashboardService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.UserService;
import com.rnpc.inventory.entity.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class AdminDashboardController {
    private final AdminDashboardService dashboard;
    private final NotificationService notifications;
    private final UserService users;
    public AdminDashboardController(AdminDashboardService dashboard, NotificationService notifications, UserService users) {
        this.dashboard = dashboard; this.notifications = notifications; this.users = users;
    }

    @GetMapping("/admin/dashboard")
    public String show(Authentication authentication, @RequestParam(defaultValue="7") int days, Model model) {
        // Guard before any shop-wide service reads; the global security chain permits all routes.
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        LocalDateTime now = LocalDateTime.now();
        model.addAllAttributes(dashboard.load(now, days));
        String name = users.findByUsername(authentication.getName()).map(User::getFullName)
                .filter(n -> !n.isBlank()).orElse(authentication.getName());
        model.addAttribute("currentUsername", name);
        model.addAttribute("currentRole", "Admin");
        model.addAttribute("navIsAdmin", true);
        model.addAttribute("unreadNotifications", notifications.getUnreadCountForAdmin());
        model.addAttribute("recentNotifications", notifications.getForAdmin().stream().limit(15).toList());
        model.addAttribute("greeting", now.getHour() < 12 ? "Good morning," : now.getHour() < 18 ? "Good afternoon," : "Good evening,");
        model.addAttribute("todayLabel", now.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
        model.addAttribute("todayDay", now.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)));
        model.addAttribute("timeLabel", now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)));
        return "admin/dashboard";
    }
}

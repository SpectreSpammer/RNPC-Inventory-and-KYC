package com.rnpc.inventory.service;

import com.rnpc.inventory.entity.Notification;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repo;

    @Autowired
    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    // Broadcast to the single shared admin inbox - every account with ROLE_ADMIN sees it.
    // entityType/entityId let the notification list pop up that exact record's own detail modal
    // instead of navigating away - pass both null for a notification with no specific record.
    public void notifyAdmin(String message, Notification.EntityType entityType, Long entityId) {
        Notification notification = new Notification();
        notification.setAudience(Notification.Audience.ADMIN);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setCreatedAt(new Date());
        repo.save(notification);
    }

    // No-ops when user is null (e.g. a walk-in Client never linked to a logged-in account) -
    // there's nobody to deliver it to.
    public void notifyCustomer(User user, String message, Notification.EntityType entityType, Long entityId) {
        if (user == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setAudience(Notification.Audience.CUSTOMER);
        notification.setUser(user);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setCreatedAt(new Date());
        repo.save(notification);
    }

    public List<Notification> getForAdmin() {
        return repo.findByAudienceOrderByCreatedAtDesc(Notification.Audience.ADMIN);
    }

    public List<Notification> getForUser(String username) {
        return repo.findByUser_UsernameOrderByCreatedAtDesc(username);
    }

    public long getUnreadCountForAdmin() {
        return repo.countByAudienceAndIsReadFalse(Notification.Audience.ADMIN);
    }

    public long getUnreadCountForUser(String username) {
        return repo.countByUser_UsernameAndIsReadFalse(username);
    }

    public void markAllReadForAdmin() {
        List<Notification> notifications = getForAdmin();
        notifications.forEach(n -> n.setRead(true));
        repo.saveAll(notifications);
    }

    public void markAllReadForUser(String username) {
        List<Notification> notifications = getForUser(username);
        notifications.forEach(n -> n.setRead(true));
        repo.saveAll(notifications);
    }

    // Display-only formatting for Notification.createdAt (Notification.java:43) - no existing
    // page in this app shows relative time (notificationIndex.html:77 shows an absolute
    // timestamp), so this is new, but it's a pure function with no query behind it. Static so the
    // topbar dropdown can call it directly via
    // T(com.rnpc.inventory.service.NotificationService).relativeTime(...), same pattern
    // DashboardController's own static display helpers already use.
    public static String relativeTime(Date date) {
        if (date == null) {
            return "";
        }
        long seconds = (System.currentTimeMillis() - date.getTime()) / 1000;
        if (seconds < 60) {
            return "Just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = hours / 24;
        if (days < 30) {
            return days + (days == 1 ? " day ago" : " days ago");
        }
        long months = days / 30;
        if (months < 12) {
            return months + (months == 1 ? " month ago" : " months ago");
        }
        long years = months / 12;
        return years + (years == 1 ? " year ago" : " years ago");
    }
}

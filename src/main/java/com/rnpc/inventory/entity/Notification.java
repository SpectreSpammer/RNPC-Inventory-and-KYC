package com.rnpc.inventory.entity;

import jakarta.persistence.*;

import java.util.Date;

// A single shared inbox for ADMIN notifications (user == null, anyone with ROLE_ADMIN sees it),
// or a CUSTOMER notification addressed to one specific User (set from Client.user - see
// NotificationService). There's no per-admin-account inbox since the app has no concept of
// multiple distinct admin teams yet.
@Entity
@Table(name = "rnpc_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Audience audience;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 500)
    private String message;

    // What this notification is about, so clicking it can pop up that exact order/repair/
    // appointment's own detail modal instead of dumping the viewer onto a whole separate page -
    // with hundreds of notifications piling up over time, losing your place in the list to chase
    // down one record defeats the point of a notification feed. Both null for a notification with
    // no specific record behind it.
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    private Long entityId;

    @Column(nullable = false)
    private boolean isRead = false;

    private Date createdAt;

    public enum Audience {
        ADMIN, CUSTOMER
    }

    public enum EntityType {
        ORDER, REPAIR, APPOINTMENT
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

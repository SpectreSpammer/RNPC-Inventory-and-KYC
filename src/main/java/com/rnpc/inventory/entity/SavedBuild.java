package com.rnpc.inventory.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Direct User FK (not through Client, unlike Order) - a saved build is a signed-in account's own
// workspace item, never something a guest checkout needs to attach to a Client record. Same
// pattern as Notification.java:23-25.
@Entity
@Table(name = "rnpc_saved_builds")
public class SavedBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long savedBuildId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private double totalPrice;

    private Date createdAt;

    @OneToMany(mappedBy = "savedBuild", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavedBuildItem> items = new ArrayList<>();

    public Long getSavedBuildId() {
        return savedBuildId;
    }

    public void setSavedBuildId(Long savedBuildId) {
        this.savedBuildId = savedBuildId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<SavedBuildItem> getItems() {
        return items;
    }

    public void setItems(List<SavedBuildItem> items) {
        this.items = items;
    }
}

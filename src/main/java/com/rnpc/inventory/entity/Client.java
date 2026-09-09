package com.rnpc.inventory.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "rnpc_clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientId;

    private String fullName;
    private String contactNumber;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    private Date createdAt;
    private String imageFileName;

    // Set when this client record was created (or first re-used) while someone was logged in -
    // it's how a customer's own appointments/repair history get scoped to their account, since
    // Client rows are otherwise looked up anonymously by contact number. Null for walk-ins that
    // an admin entered directly (e.g. via /ticket/create) without a logged-in customer involved.
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

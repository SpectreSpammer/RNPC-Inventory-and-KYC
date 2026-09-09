package com.rnpc.inventory.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    // Null for Google-only accounts - they authenticate via OAuth2, never a local password.
    private String password;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Profile fields, editable via /profile/edit. Null until the account owner sets them - the
    // seeded demo accounts and freshly-created Google accounts don't have these yet.
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String contactNumber;

    // Google's opaque, globally-unique subject id ("sub" claim) - set the first time this account
    // signs in via Google (see CustomOidcUserService). Null for accounts that have never signed in
    // with Google (e.g. the seeded local admin/12345 account).
    @Column(unique = true)
    private String googleId;

    // Constructors
    public User() {}

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    // Not a persisted column (field access means Hibernate never sees this as a mapped property).
    // The account's Google subject id when it has one (a real, globally-unique identifier), else
    // its own internal database id as a last resort for accounts that only ever sign in locally.
    @Transient
    public String getEmployeeId() {
        return googleId != null ? googleId : String.valueOf(id);
    }

    // A human-readable "signed by" label for admin-action attribution (Mark Paid, Mark Refunded,
    // etc.) - the admin's display name plus the last 5 digits of their employee id, e.g.
    // "Test Admin-12345", rather than the raw (and for a Google id, very long) id on its own.
    @Transient
    public String getEmployeeLabel() {
        String id = getEmployeeId();
        String last5 = id.length() > 5 ? id.substring(id.length() - 5) : id;
        String name = (fullName != null && !fullName.isBlank()) ? fullName : username;
        return name + "-" + last5;
    }

    // Role enum
    public enum Role {
        ADMIN, CUSTOMER
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
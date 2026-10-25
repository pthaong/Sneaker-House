package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
    @UniqueConstraint(name = "uk_users_google_sub", columnNames = "google_sub")
})
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;
    @Column(name = "password_hash", length = 100)
    private String passwordHash;
    @Column(name = "google_sub", length = 255, unique = true)
    private String googleSub;
    @Column(nullable = false, length = 30)
    private String role = "CUSTOMER";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() {
        createdAt = Instant.now();
        email = email == null ? null : email.trim().toLowerCase();
    }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}

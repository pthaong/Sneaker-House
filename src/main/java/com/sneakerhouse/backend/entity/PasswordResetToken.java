package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens", indexes = @Index(name = "idx_reset_token_hash", columnList = "token_hash", unique = true))
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    protected PasswordResetToken() {}
    public PasswordResetToken(UserAccount user, String tokenHash, Instant expiresAt) {
        this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }
    public UserAccount getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void markUsed() { usedAt = Instant.now(); }
}

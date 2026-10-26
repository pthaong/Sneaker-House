package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = @Index(
        name = "idx_reset_token_hash",
        columnList = "token_hash",
        unique = true
    )
)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "reset_grant_hash", length = 64)
    private String resetGrantHash;

    @Column(name = "reset_grant_expires_at")
    private Instant resetGrantExpiresAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(
            UserAccount user,
            String tokenHash,
            Instant expiresAt
    ) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public String getResetGrantHash() {
        return resetGrantHash;
    }

    public Instant getResetGrantExpiresAt() {
        return resetGrantExpiresAt;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public void markVerified(
            String resetGrantHash,
            Instant resetGrantExpiresAt
    ) {
        this.verifiedAt = Instant.now();
        this.resetGrantHash = resetGrantHash;
        this.resetGrantExpiresAt = resetGrantExpiresAt;
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
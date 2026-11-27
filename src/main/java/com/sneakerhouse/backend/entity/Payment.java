
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(
                        name = "idx_payment_provider_order_code",
                        columnList = "provider_order_code",
                        unique = true
                ),
                @Index(
                        name = "idx_payment_order_created",
                        columnList = "order_id, created_at"
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    public CustomerOrder order;

    @Column(name = "payment_method", nullable = false, length = 20)
    public String paymentMethod = "PAYOS";

    @Column(name = "transaction_code", nullable = false, unique = true, length = 100)
    public String transactionCode;

    @Column(name = "provider_order_code", unique = true)
    public Long providerOrderCode;

    @Column(name = "payment_link_id", length = 100)
    public String paymentLinkId;

    @Column(nullable = false, precision = 18, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false, length = 20)
    public String status = "PENDING";

    @Column(name = "provider_transaction_id", length = 100)
    public String providerTransactionId;

    @Column(name = "payment_url", length = 2000)
    public String paymentUrl;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

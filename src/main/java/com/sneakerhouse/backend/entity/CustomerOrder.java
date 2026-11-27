
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    public String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public UserAccount user;

    @Column(name = "recipient_name", nullable = false, length = 120)
    public String recipientName;

    @Column(name = "phone_number", nullable = false, length = 20)
    public String phoneNumber;

    @Column(nullable = false, length = 100)
    public String province;

    @Column(nullable = false, length = 100)
    public String district;

    @Column(nullable = false, length = 100)
    public String ward;

    @Column(name = "detail_address", nullable = false, length = 255)
    public String detailAddress;

    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    public BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    public BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 2)
    public BigDecimal shippingFee;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    public BigDecimal totalAmount;

    @Column(name = "coupon_code", length = 50)
    public String couponCode;

    @Column(name = "payment_method", nullable = false, length = 30)
    public String paymentMethod = "COD";

    @Column(name = "payment_status", nullable = false, length = 30)
    public String paymentStatus = "UNPAID";

    @Column(nullable = false, length = 30)
    public String status = "PENDING";

    @Column(length = 1000)
    public String note;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}

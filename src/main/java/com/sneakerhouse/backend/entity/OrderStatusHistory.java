
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "order_status_histories")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    public CustomerOrder order;

    @Column(name = "old_status", length = 30)
    public String oldStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    public String newStatus;

    @Column(length = 500)
    public String note;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }
}


package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "product_variants",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_variant_size_color",
            columnNames = {"product_id", "size", "color"}
        )
    }
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    public Product product;

    @Nationalized
    @Column(name = "size", nullable = false, length = 30)
    public String size;

    @Nationalized
    @Column(name = "color", nullable = false, length = 50)
    public String color;

    @Column(name = "stock_quantity", nullable = false)
    public Integer stockQuantity = 0;

    @Column(nullable = false)
    public Boolean active = true;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

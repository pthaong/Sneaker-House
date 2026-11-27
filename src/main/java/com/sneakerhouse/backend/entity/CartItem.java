
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_cart_product_variant",
        columnNames = {"cart_id", "product_id", "size", "color"}
    )
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    public Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    public Product product;

    // Biến thể size/màu của sản phẩm.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    public ProductVariant variant;

    @Column(nullable = false)
    public Integer quantity = 1;

    @Column(name = "size", length = 30)
    public String size;

    @Column(name = "color", length = 50)
    public String color;

    @Column(name = "selected", nullable = false)
    public Boolean selected = true;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}

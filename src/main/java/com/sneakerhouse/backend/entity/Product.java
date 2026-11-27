
package com.sneakerhouse.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Nationalized
    @Column(nullable = false, length = 180)
    public String name;

    @Column(nullable = false, length = 60, unique = true)
    public String sku;

    @Nationalized
    @Column(length = 180, unique = true)
    public String slug;

    @Nationalized
    @Column(length = 2000)
    public String description;

    @Column(name = "image_url", length = 600)
    public String imageUrl;

    @Column(name = "original_price",
            nullable = false, precision = 18, scale = 2)
    public BigDecimal originalPrice;

    @Column(name = "sale_price", precision = 18, scale = 2)
    public BigDecimal salePrice;

    @Column(name = "stock_quantity", nullable = false)
    public Integer stockQuantity = 0;

    @Nationalized
    @Column(length = 200)
    public String sizes;

    @Nationalized
    @Column(length = 300)
    public String colors;

    @Column(name = "sold_count", nullable = false)
    public Integer soldCount = 0;

    @Column(nullable = false)
    public Boolean featured = false;

    @Column(nullable = false)
    public Boolean active = true;

    @Column(name = "flash_sale_end")
    public LocalDateTime flashSaleEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    public Brand brand;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}

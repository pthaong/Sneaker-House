package com.sneakerhouse.backend.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDateTime;
@Entity @Table(name="products") public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=180) public String name;
 @Column(nullable=false,length=60,unique=true) public String sku;
 @Column(length=1000) public String description;
 @Column(name="image_url",length=600) public String imageUrl;
 @Column(name="original_price",nullable=false,precision=18,scale=2) public BigDecimal originalPrice;
 @Column(name="sale_price",precision=18,scale=2) public BigDecimal salePrice;
 @Column(name="stock_quantity",nullable=false) public Integer stockQuantity=0;
 @Column(name="sizes",length=200) public String sizes;
 @Column(name="colors",length=300) public String colors;
 @Column(name="sold_count",nullable=false) public Integer soldCount=0;
 @Column(name="featured",nullable=false) public Boolean featured=false;
 @Column(name="active",nullable=false) public Boolean active=true;
 @Column(name="flash_sale_end") public LocalDateTime flashSaleEnd;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="category_id") public Category category;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="brand_id") public Brand brand;
 @Column(name="created_at",nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
}

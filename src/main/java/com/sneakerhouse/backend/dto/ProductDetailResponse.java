
package com.sneakerhouse.backend.dto;

import com.sneakerhouse.backend.entity.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record ProductDetailResponse(
        Long id,
        String name,
        String slug,
        String sku,
        String description,
        String imageUrl,
        String brand,
        String category,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        BigDecimal finalPrice,
        Integer discountPercent,
        Integer stockQuantity,
        String sizes,
        String colors,
        Integer soldCount,
        Boolean featured,
        LocalDateTime createdAt
) {
    public static ProductDetailResponse from(Product p) {
        BigDecimal original = p.originalPrice != null
                ? p.originalPrice : BigDecimal.ZERO;

        BigDecimal finalPrice = p.salePrice != null
                && p.salePrice.compareTo(original) < 0
                ? p.salePrice : original;

        int discount = original.signum() > 0
                ? original.subtract(finalPrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(original, 0, RoundingMode.HALF_UP)
                    .intValue()
                : 0;

        return new ProductDetailResponse(
                p.id,
                p.name,
                p.slug,
                p.sku,
                p.description,
                p.imageUrl,
                p.brand == null ? null : p.brand.name,
p.category == null ? null : p.category.name,
                p.originalPrice,
                p.salePrice,
                finalPrice,
                discount,
                p.stockQuantity,
                p.sizes,
                p.colors,
                p.soldCount,
                p.featured,
                p.createdAt
        );
    }
}

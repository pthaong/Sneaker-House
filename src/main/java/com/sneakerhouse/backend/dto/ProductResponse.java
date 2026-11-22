
package com.sneakerhouse.backend.dto;

import com.sneakerhouse.backend.entity.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String imageUrl,
        String brand,
        String category,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        Integer discountPercent,
        Integer stockQuantity,
        String sizes,
        String colors,
        Integer soldCount,
        LocalDateTime flashSaleEnd,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product p) {

        BigDecimal original = p.originalPrice != null
                ? p.originalPrice
                : BigDecimal.ZERO;

        BigDecimal price = p.salePrice != null
                && p.salePrice.compareTo(original) < 0
                ? p.salePrice
                : original;

        int discount = original.signum() > 0
                ? original.subtract(price)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(original, 0, RoundingMode.HALF_UP)
                    .intValue()
                : 0;

        return new ProductResponse(
                p.id,
                p.name,
                p.sku,
                p.imageUrl,
                p.brand == null ? null : p.brand.name,
p.category == null ? null : p.category.name,
                original,
                p.salePrice,
                discount,
                p.stockQuantity,
                p.sizes,
                p.colors,
                p.soldCount,
                p.flashSaleEnd,
                p.createdAt
        );
    }
}

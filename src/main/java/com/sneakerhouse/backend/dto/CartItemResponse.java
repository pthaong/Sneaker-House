
package com.sneakerhouse.backend.dto;

import com.sneakerhouse.backend.entity.CartItem;
import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String name,
        String sku,
        String imageUrl,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        BigDecimal finalPrice,
        Integer quantity,
        String size,
        String color,
        Boolean selected,
        Integer stockQuantity,
        BigDecimal lineTotal
) {
    public static CartItemResponse from(CartItem item) {
        var product = item.product;

        BigDecimal originalPrice = product.originalPrice == null
                ? BigDecimal.ZERO : product.originalPrice;

        BigDecimal finalPrice = product.salePrice != null
                && product.salePrice.compareTo(originalPrice) < 0
                ? product.salePrice : originalPrice;

        return new CartItemResponse(
                item.id,
                product.id,
                product.name,
                product.sku,
                product.imageUrl,
                product.originalPrice,
                product.salePrice,
                finalPrice,
                item.quantity,
                item.size,
                item.color,
                item.selected,
                product.stockQuantity,
                finalPrice.multiply(BigDecimal.valueOf(item.quantity))
        );
    }
}

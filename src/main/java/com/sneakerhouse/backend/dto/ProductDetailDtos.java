
package com.sneakerhouse.backend.dto;

import com.sneakerhouse.backend.entity.Product;
import com.sneakerhouse.backend.entity.ProductVariant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

public final class ProductDetailDtos {

    private ProductDetailDtos() {}

    public record VariantResponse(
            Long id,
            String size,
            String color,
            Integer stockQuantity,
            Boolean available
    ) {
        public static VariantResponse from(ProductVariant variant) {
            int stock = variant.stockQuantity == null
                    ? 0 : variant.stockQuantity;

            return new VariantResponse(
                    variant.id,
                    variant.size,
                    variant.color,
                    stock,
                    stock > 0
            );
        }
    }

    public record ProductDetailResponse(
            Long id,
            String name,
            String sku,
            String slug,
            String description,
            List<String> images,
            Long categoryId,
            String categoryName,
            Long brandId,
            String brandName,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            BigDecimal discountPercent,
            Integer stockQuantity,
            List<String> sizes,
            List<String> colors,
            List<VariantResponse> variants,
            Integer soldCount,
            Boolean featured
    ) {
        public static ProductDetailResponse from(
                Product p,
                List<ProductVariant> variants
        ) {
            BigDecimal sellingPrice =
                    p.salePrice != null ? p.salePrice : p.originalPrice;

            BigDecimal discount = BigDecimal.ZERO;

            if (p.originalPrice != null
                    && p.originalPrice.compareTo(BigDecimal.ZERO) > 0
                    && sellingPrice != null
                    && sellingPrice.compareTo(p.originalPrice) < 0) {
                discount = p.originalPrice.subtract(sellingPrice)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(p.originalPrice, 2, RoundingMode.HALF_UP);
            }

            List<String> images = p.imageUrl == null || p.imageUrl.isBlank()
                    ? List.of()
                    : List.of(p.imageUrl);

            int totalVariantStock = variants.stream()
                    .mapToInt(v -> v.stockQuantity == null
                            ? 0 : v.stockQuantity)
                    .sum();

            return new ProductDetailResponse(
                    p.id,
                    p.name,
                    p.sku,
                    p.slug,
                    p.description,
                    images,
                    p.category == null ? null : p.category.id,
                    p.category == null ? null : p.category.name,
                    p.brand == null ? null : p.brand.id,
                    p.brand == null ? null : p.brand.name,
                    p.originalPrice,
                    sellingPrice,
                    discount,
                    variants.isEmpty()
                            ? p.stockQuantity : totalVariantStock,
                    splitValues(p.sizes),
                    splitValues(p.colors),
                    variants.stream()
                            .map(VariantResponse::from)
                            .toList(),
                    p.soldCount,
                    p.featured
            );
        }

        private static List<String> splitValues(String value) {
            if (value == null || value.isBlank()) {
                return List.of();
            }

            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .distinct()
                    .toList();
        }
    }
}

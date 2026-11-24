
package com.sneakerhouse.backend.dto;

public record BrandResponse(
        Long id,
        String name,
        String logoUrl,
        Long productCount
) {
}

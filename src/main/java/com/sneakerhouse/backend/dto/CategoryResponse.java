
package com.sneakerhouse.backend.dto;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        Long parentId,
        Long productCount,
        List<CategoryResponse> children
) {
}

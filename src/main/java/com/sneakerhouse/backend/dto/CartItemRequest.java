
package com.sneakerhouse.backend.dto;

public record CartItemRequest(
        Long productId,
        Integer quantity,
        String size,
        String color
) {
}

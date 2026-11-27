
package com.sneakerhouse.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        Integer totalItems,
        Integer selectedItems,
        BigDecimal subtotal
) {
}

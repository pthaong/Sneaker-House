
package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class ShippingDtos {

    private ShippingDtos() {}

    public record ShippingRequest(
            @NotNull
            @DecimalMin("0.01")
            BigDecimal orderAmount
    ) {}

    public record ShippingResponse(
            BigDecimal orderAmount,
            BigDecimal shippingFee,
            BigDecimal finalAmount,
            boolean freeShipping,
            String message
    ) {}
}

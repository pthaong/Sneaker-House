
package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public final class CouponDtos {

    private CouponDtos() {}

    public record CouponCheckRequest(
            @NotBlank
            @Size(max = 50)
            String code,

            @NotNull
            @DecimalMin("0.01")
            BigDecimal orderAmount
    ) {}

    public record CouponCheckResponse(
            String code,
            String discountType,
            BigDecimal discountValue,
            BigDecimal orderAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String message
    ) {}

    public record CouponCreateRequest(
            @NotBlank @Size(max = 50)
            String code,

            @NotBlank
            @Pattern(regexp = "PERCENT|FIXED")
            String discountType,

            @NotNull @DecimalMin("0.01")
            BigDecimal discountValue,

            @DecimalMin("0")
            BigDecimal minOrderValue,

            @DecimalMin("0")
            BigDecimal maxDiscount,

            @Min(1)
            Integer usageLimit,

            Instant startAt,

            Instant endAt
    ) {}
}

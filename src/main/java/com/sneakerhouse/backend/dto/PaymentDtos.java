
package com.sneakerhouse.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentDtos {

    private PaymentDtos() {}

    public record CreatePaymentResponse(
            Long paymentId,
            Long orderId,
            Long providerOrderCode,
            String paymentMethod,
            BigDecimal amount,
            String status,
            String paymentUrl
    ) {}

    public record PaymentStatusResponse(
            Long paymentId,
            Long orderId,
            String paymentMethod,
            BigDecimal amount,
            String status,
            String orderPaymentStatus,
            String paymentUrl,
            LocalDateTime createdAt
    ) {}
}


package com.sneakerhouse.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    public record CreateOrderRequest(
            @NotNull Long addressId,

            @Size(max = 50)
            String couponCode,

            @Size(max = 1000)
            String note,

            @Pattern(
                    regexp = "(?i)COD|PAYOS",
                    message = "Phương thức thanh toán chỉ hỗ trợ COD hoặc PAYOS."
            )
            String paymentMethod
    ) {}

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            String productSku,
            String imageUrl,
            String size,
            String color,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record OrderResponse(
            Long id,
            String orderCode,
            String status,
            String paymentMethod,
            String paymentStatus,
            String recipientName,
            String phoneNumber,
            String province,
            String district,
            String ward,
            String detailAddress,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String couponCode,
            String note,
            Instant createdAt,
            List<OrderItemResponse> items
    ) {}

    public record OrderHistoryResponse(
            Long id,
            String orderCode,
            String status,
            String paymentMethod,
            String paymentStatus,
            BigDecimal totalAmount,
            Instant createdAt
    ) {}

    public record OrderStatusHistoryResponse(
            String oldStatus,
            String newStatus,
            String note,
            Instant createdAt
    ) {}
}


package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.ShippingDtos;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ShippingService {

    // Phí vận chuyển mặc định.
    private static final BigDecimal SHIPPING_FEE =
            new BigDecimal("30000");

    // Đơn hàng từ 1.000.000đ được miễn phí vận chuyển.
    private static final BigDecimal FREE_SHIPPING_LIMIT =
            new BigDecimal("1000000");

    public ShippingDtos.ShippingResponse calculateShipping(
            ShippingDtos.ShippingRequest request
    ) {

        if (request == null || request.orderAmount() == null
                || request.orderAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Giá trị đơn hàng không được nhỏ hơn 0."
            );
        }

        BigDecimal orderAmount = request.orderAmount();

        boolean freeShipping =
                orderAmount.compareTo(FREE_SHIPPING_LIMIT) >= 0;

        BigDecimal shippingFee = freeShipping
                ? BigDecimal.ZERO
                : SHIPPING_FEE;

        BigDecimal finalAmount = orderAmount.add(shippingFee);

        String message = freeShipping
                ? "Đơn hàng được miễn phí vận chuyển."
                : "Phí vận chuyển là 30.000đ.";

        return new ShippingDtos.ShippingResponse(
                orderAmount,
                shippingFee,
                finalAmount,
                freeShipping,
                message
        );
    }
}

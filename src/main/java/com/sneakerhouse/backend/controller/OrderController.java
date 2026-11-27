
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.OrderDtos;
import com.sneakerhouse.backend.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

   // Tạo đơn hàng COD hoặc PAYOS
    @PostMapping
    public ResponseEntity<OrderDtos.OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody OrderDtos.CreateOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(authentication, request));
    }

    // Danh sách đơn hàng của tài khoản đang đăng nhập
    @GetMapping
    public ResponseEntity<List<OrderDtos.OrderHistoryResponse>> getMyOrders(
            Authentication authentication
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication));
    }

    // Chi tiết đơn hàng
    @GetMapping("/{id}")
    public ResponseEntity<OrderDtos.OrderResponse> getOrderDetail(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                orderService.getOrderDetail(authentication, id)
        );
    }

    // Hủy đơn hàng
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDtos.OrderResponse> cancelOrder(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                orderService.cancelOrder(authentication, id)
        );
    }

    // Lịch sử trạng thái đơn hàng
    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<OrderDtos.OrderStatusHistoryResponse>>
    getOrderStatusHistory(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                orderService.getOrderStatusHistory(authentication, id)
        );
    }
}

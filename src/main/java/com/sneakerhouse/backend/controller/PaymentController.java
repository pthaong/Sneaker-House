
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.PaymentDtos;
import com.sneakerhouse.backend.service.PayOSService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PayOSService payOSService;

    public PaymentController(PayOSService payOSService) {
        this.payOSService = payOSService;
    }

    @PostMapping("/payos/{orderId}")
    public ResponseEntity<PaymentDtos.CreatePaymentResponse> createPayOSPayment(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                payOSService.createPayment(authentication, orderId)
        );
    }

    @PostMapping(
            value = "/payos/webhook",
            consumes = "application/json"
    )
    public ResponseEntity<Map<String, Object>> payOSWebhook(
            @RequestBody String rawBody
    ) {
        payOSService.handleWebhook(rawBody);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook received"
        ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentDtos.PaymentStatusResponse> getPaymentStatus(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                payOSService.getPaymentStatus(authentication, orderId)
        );
    }
}

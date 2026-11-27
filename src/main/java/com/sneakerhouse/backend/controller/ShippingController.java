
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ShippingDtos;
import com.sneakerhouse.backend.service.ShippingService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ShippingDtos.ShippingResponse> calculate(
            @Valid @RequestBody ShippingDtos.ShippingRequest request) {

        return ResponseEntity.ok(
                shippingService.calculateShipping(request)
        );
    }
}

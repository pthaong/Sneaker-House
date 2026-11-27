
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.CouponDtos.*;
import com.sneakerhouse.backend.entity.Coupon;
import com.sneakerhouse.backend.service.CouponService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    // Kiểm tra mã giảm giá
    @PostMapping("/check")
    public CouponCheckResponse checkCoupon(
            @Valid @RequestBody CouponCheckRequest request
    ) {
        return service.checkCoupon(request);
    }

    // Tạo mã giảm giá (cần quyền ADMIN)
    @PostMapping
    public ResponseEntity<Coupon> createCoupon(
            @Valid @RequestBody CouponCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createCoupon(request));
    }
}

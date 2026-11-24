
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ProductReviewDtos.*;
import com.sneakerhouse.backend.service.ProductReviewService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {

    private final ProductReviewService service;

    public ProductReviewController(ProductReviewService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReviewResponse> getReviews(
            @PathVariable Long productId
    ) {
        return service.getReviews(productId);
    }

    @GetMapping("/summary")
    public RatingSummary getSummary(
            @PathVariable Long productId
    ) {
        return service.getSummary(productId);
    }

    @PostMapping
    public ReviewResponse create(
            @PathVariable Long productId,
            @Valid @RequestBody CreateRequest request,
            Authentication authentication
    ) {
        return service.create(productId, request, authentication);
    }
}

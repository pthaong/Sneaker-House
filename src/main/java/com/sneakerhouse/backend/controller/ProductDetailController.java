
package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.ProductDetailDtos.ProductDetailResponse;
import com.sneakerhouse.backend.service.ProductDetailService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-details")
public class ProductDetailController {

    private final ProductDetailService service;

    public ProductDetailController(ProductDetailService service) {
        this.service = service;
    }

    // Chi tiết sản phẩm theo ID
    @GetMapping("/{id}")
    public ProductDetailResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // Chi tiết sản phẩm theo slug
    @GetMapping("/slug/{slug}")
    public ProductDetailResponse getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }

    // Sản phẩm liên quan
    @GetMapping("/{id}/related")
    public List<ProductDetailResponse> getRelated(@PathVariable Long id) {
        return service.getRelated(id);
    }
}


package com.sneakerhouse.backend.controller;

import com.sneakerhouse.backend.dto.BrandResponse;
import com.sneakerhouse.backend.entity.Brand;
import com.sneakerhouse.backend.repository.BrandRepository;
import com.sneakerhouse.backend.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandController(
            BrandRepository brandRepository,
            ProductRepository productRepository
    ) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    // Danh sách thương hiệu
    @GetMapping
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {

        return brandRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Chi tiết thương hiệu
    @GetMapping("/{id:\\d+}")
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(@PathVariable Long id) {

        Brand brand = brandRepository.findById(id)
                .filter(b -> Boolean.TRUE.equals(b.active))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy thương hiệu"
                ));

        return toResponse(brand);
    }

    private BrandResponse toResponse(Brand brand) {

        long productCount =
                productRepository.countByBrand_IdAndActiveTrue(brand.id);

        return new BrandResponse(
                brand.id,
                brand.name,
                brand.logoUrl,
                productCount
        );
    }
}

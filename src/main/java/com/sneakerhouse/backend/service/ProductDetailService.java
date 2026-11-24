
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.dto.ProductDetailDtos.ProductDetailResponse;
import com.sneakerhouse.backend.entity.Product;
import com.sneakerhouse.backend.repository.ProductRepository;
import com.sneakerhouse.backend.repository.ProductVariantRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductDetailService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;

    public ProductDetailService(
            ProductRepository products,
            ProductVariantRepository variants
    ) {
        this.products = products;
        this.variants = variants;
    }

    public ProductDetailResponse getById(Long id) {
        Product product = findActiveProduct(id);

        return ProductDetailResponse.from(
                product,
                variants.findByProduct_IdAndActiveTrue(id)
        );
    }

    public ProductDetailResponse getBySlug(String slug) {
        Product product = products.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.active))
                .filter(p -> p.slug != null
                        && p.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm"
                ));

        return getById(product.id);
    }

    public List<ProductDetailResponse> getRelated(Long productId) {
        Product current = findActiveProduct(productId);

        if (current.category == null) {
            return List.of();
        }

        return products.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.active))
                .filter(p -> !p.id.equals(current.id))
                .filter(p -> p.category != null
                        && p.category.id.equals(current.category.id))
                .sorted((a, b) -> Integer.compare(
                        b.soldCount == null ? 0 : b.soldCount,
                        a.soldCount == null ? 0 : a.soldCount
                ))
                .limit(8)
                .map(p -> ProductDetailResponse.from(
                        p,
                        variants.findByProduct_IdAndActiveTrue(p.id)
                ))
                .toList();
    }

    private Product findActiveProduct(Long id) {
        return products.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.active))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm"
                ));
    }
}

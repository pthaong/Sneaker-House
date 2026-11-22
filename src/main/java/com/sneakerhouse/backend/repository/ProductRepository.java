
package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    List<Product> findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    List<Product> findTop8ByActiveTrueOrderByCreatedAtDesc();

    List<Product> findTop8ByActiveTrueOrderBySoldCountDesc();

    List<Product> findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(
            LocalDateTime now
    );

    Optional<Product> findBySlugAndActiveTrue(String slug);

    // Đếm sản phẩm theo thương hiệu
    long countByBrand_IdAndActiveTrue(Long brandId);

    // Đếm sản phẩm theo danh mục
    long countByCategory_IdAndActiveTrue(Long categoryId);
}

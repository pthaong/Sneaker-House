
package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.Product;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    // Đếm sản phẩm đang hoạt động theo thương hiệu.
    long countByBrand_IdAndActiveTrue(Long brandId);

    // Đếm sản phẩm đang hoạt động theo danh mục.
    long countByCategory_IdAndActiveTrue(Long categoryId);

    // Sản phẩm nổi bật mới nhất.
    List<Product> findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    // 8 sản phẩm mới nhất.
    List<Product> findTop8ByActiveTrueOrderByCreatedAtDesc();

    // 8 sản phẩm bán chạy.
    List<Product> findTop8ByActiveTrueOrderBySoldCountDesc();

    // Sản phẩm đang trong chương trình flash sale.
    List<Product> findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(
            LocalDateTime now
    );

    // Tìm sản phẩm theo slug.
    Optional<Product> findBySlugAndActiveTrue(String slug);

    // Khóa sản phẩm khi xử lý trừ tồn kho lúc đặt hàng.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}


package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository
        extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProduct_IdOrderByCreatedAtDesc(
            Long productId
    );

    Optional<ProductReview> findByProduct_IdAndUser_Id(
            Long productId,
            Long userId
    );

    @Query("""
        SELECT AVG(r.rating)
        FROM ProductReview r
        WHERE r.product.id = :productId
    """)
    Double getAverageRating(@Param("productId") Long productId);

    long countByProduct_Id(Long productId);
}

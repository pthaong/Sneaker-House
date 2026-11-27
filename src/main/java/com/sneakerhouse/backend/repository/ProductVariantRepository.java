
package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct_IdAndActiveTrue(Long productId);

    Optional<ProductVariant>
    findByProduct_IdAndSizeIgnoreCaseAndColorIgnoreCaseAndActiveTrue(
            Long productId,
            String size,
            String color
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT v FROM ProductVariant v
        WHERE v.id = :id AND v.active = true
    """)
    Optional<ProductVariant> findActiveByIdForUpdate(
            @Param("id") Long id
    );
}

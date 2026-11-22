
package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    // San pham noi bat
    List<Product>
    findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    // San pham moi ve
    List<Product>
    findTop8ByActiveTrueOrderByCreatedAtDesc();

    // San pham ban chay
    List<Product>
    findTop8ByActiveTrueOrderBySoldCountDesc();

    // Flash Sale con han, con hang va co gia giam hop le
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.active = true
          AND p.flashSaleEnd > :now
          AND p.stockQuantity > 0
          AND p.salePrice IS NOT NULL
          AND p.salePrice < p.originalPrice
        ORDER BY p.flashSaleEnd ASC
        """)
    List<Product>
    findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(
            @Param("now") LocalDateTime now
    );
}

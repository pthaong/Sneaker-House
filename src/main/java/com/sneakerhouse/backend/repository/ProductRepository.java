package com.sneakerhouse.backend.repository;
import com.sneakerhouse.backend.entity.Product; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.JpaSpecificationExecutor; import java.util.List;
public interface ProductRepository extends JpaRepository<Product,Long>, JpaSpecificationExecutor<Product>{
 List<Product> findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();
 List<Product> findTop8ByActiveTrueOrderByCreatedAtDesc();
 List<Product> findTop8ByActiveTrueOrderBySoldCountDesc();
 List<Product> findByActiveTrueAndFlashSaleEndAfterOrderByFlashSaleEndAsc(java.time.LocalDateTime now);
}

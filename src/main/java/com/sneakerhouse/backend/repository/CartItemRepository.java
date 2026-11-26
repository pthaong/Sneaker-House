
package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.CartItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart_IdOrderByIdDesc(Long cartId);

    Optional<CartItem> findByIdAndCart_Id(Long id, Long cartId);

    Optional<CartItem> findByCart_IdAndProduct_IdAndSizeAndColor(
            Long cartId,
            Long productId,
            String size,
            String color
    );

    // Lấy các sản phẩm được khách hàng tích chọn để đặt hàng.
    List<CartItem> findByCart_IdAndSelectedTrueOrderByIdAsc(Long cartId);

    void deleteByIdAndCart_Id(Long id, Long cartId);
}

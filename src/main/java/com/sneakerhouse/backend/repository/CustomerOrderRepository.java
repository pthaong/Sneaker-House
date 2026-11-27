
package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.CustomerOrder;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    // Danh sách đơn hàng của người dùng
    List<CustomerOrder> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // Chi tiết đơn hàng theo người dùng
    Optional<CustomerOrder> findByIdAndUser_Id(Long id, Long userId);

    // Khóa đơn hàng khi xử lý hủy để tránh hoàn kho hai lần
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o FROM CustomerOrder o
        WHERE o.id = :orderId AND o.user.id = :userId
    """)
    Optional<CustomerOrder> findByIdAndUser_IdForUpdate(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );
}

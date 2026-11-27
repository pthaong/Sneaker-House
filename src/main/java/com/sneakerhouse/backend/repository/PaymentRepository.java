
package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    Optional<Payment> findByProviderOrderCode(Long providerOrderCode);

    Optional<Payment> findFirstByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrder_IdAndStatusOrderByCreatedAtDesc(
            Long orderId,
            String status
    );

    List<Payment> findByOrder_IdOrderByCreatedAtDesc(Long orderId);
}

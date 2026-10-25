package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteByUser_Id(Long userId);
}

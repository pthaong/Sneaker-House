package com.sneakerhouse.backend.repository;

import com.sneakerhouse.backend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    Optional<UserAccount> findByGoogleSub(String googleSub);
    boolean existsByEmailIgnoreCase(String email);
}

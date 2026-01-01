package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmailAndCodeAndIsUsedFalseAndExpiresAtAfter(
        String email, String code, LocalDateTime now);
    void deleteByEmail(String email);
}


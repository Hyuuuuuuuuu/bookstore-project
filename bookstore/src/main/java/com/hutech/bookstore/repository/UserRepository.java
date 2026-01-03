package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    boolean existsByEmail(String email);

    // Thống kê user mới trong khoảng thời gian
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Thống kê tổng user tính đến thời điểm
    long countByCreatedAtBefore(LocalDateTime date);
}
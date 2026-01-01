package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Cart;
import com.hutech.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    /**
     * Tìm cart của user
     */
    @Query("SELECT c FROM Cart c WHERE c.user = :user AND c.isDeleted = false")
    Optional<Cart> findByUserAndIsDeletedFalse(@Param("user") User user);

    /**
     * Kiểm tra cart có tồn tại không
     */
    boolean existsByUserAndIsDeletedFalse(User user);
}

package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    
    // Query tìm kiếm nâng cao: Join bảng User và Address để tìm theo tên/email/sđt
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN o.user u " +
           "LEFT JOIN o.shippingAddress sa " +
           "WHERE o.isDeleted = false " +
           "AND (:search IS NULL OR :search = '' OR " +
           "    LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(sa.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:userId IS NULL OR o.user.id = :userId)")
    Page<Order> findOrdersWithFilters(
            @Param("search") String search,
            @Param("status") Order.OrderStatus status,
            @Param("userId") Long userId,
            Pageable pageable
    );

    Page<Order> findByUserAndIsDeletedFalse(User user, Pageable pageable);
}
package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    Page<Order> findByUserAndIsDeletedFalse(User user, Pageable pageable);
    
    // Tìm kiếm nâng cao
    @Query("SELECT o FROM Order o WHERE o.isDeleted = false " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:userId IS NULL OR o.user.id = :userId) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrders(@Param("search") String search, 
                            @Param("status") Order.OrderStatus status,
                            @Param("userId") Long userId,
                            Pageable pageable);

    Page<Order> findByIsDeletedFalse(Pageable pageable);

    // --- REPORT QUERIES ---

    // Tổng doanh thu trong khoảng thời gian (chỉ tính đơn đã hoàn thành/giao hàng)
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.isDeleted = false " +
           "AND (o.status = 'COMPLETED' OR o.status = 'DELIVERED') " +
           "AND o.createdAt BETWEEN :start AND :end")
    Double sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Đếm số đơn hàng trong khoảng thời gian
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Đếm số đơn hàng theo trạng thái trong khoảng thời gian
    long countByStatusAndCreatedAtBetween(Order.OrderStatus status, LocalDateTime start, LocalDateTime end);
}
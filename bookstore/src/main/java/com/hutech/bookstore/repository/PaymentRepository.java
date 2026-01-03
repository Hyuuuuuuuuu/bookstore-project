package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionCode(String transactionCode);

    // Tìm kiếm thanh toán (theo status, method, và từ khóa search)
    @Query("SELECT p FROM Payment p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:method IS NULL OR p.method = :method) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.transactionCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.order.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Payment> searchPayments(@Param("search") String search, 
                                @Param("status") Payment.PaymentStatus status,
                                @Param("method") Payment.PaymentMethod method,
                                Pageable pageable);
}
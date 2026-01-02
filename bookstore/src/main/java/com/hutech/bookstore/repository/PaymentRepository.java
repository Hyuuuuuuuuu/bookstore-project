package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Query tìm kiếm nâng cao: Join Payment -> Order -> User
    @Query("SELECT p FROM Payment p " +
           "LEFT JOIN p.order o " +
           "LEFT JOIN o.user u " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "    LOWER(CAST(p.id AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR p.status = :status)")
    Page<Payment> findPaymentsWithFilters(
            @Param("search") String search,
            @Param("status") Payment.PaymentStatus status,
            Pageable pageable
    );
}
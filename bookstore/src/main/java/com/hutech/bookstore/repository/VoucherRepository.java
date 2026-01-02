package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeAndIsDeletedFalse(String code);
    
    @Query("SELECT v FROM Voucher v WHERE v.isActive = true AND v.isDeleted = false " +
           "AND v.validFrom <= :now AND v.validTo >= :now " +
           "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    List<Voucher> findValidVouchers(@Param("now") LocalDateTime now);
    
    /**
     * Lấy tất cả voucher (active và chưa bị xóa, không filter theo thời gian)
     */
    List<Voucher> findByIsActiveTrueAndIsDeletedFalse();
    
    /**
     * Lấy tất cả voucher (chưa bị xóa)
     */
    List<Voucher> findByIsDeletedFalse();
}


package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    
    /**
     * Kiểm tra user đã sử dụng voucher cho đơn hàng cụ thể chưa
     */
    boolean existsByVoucherIdAndUserIdAndOrderId(Long voucherId, Long userId, Long orderId);
    
    /**
     * Kiểm tra user đã sử dụng voucher này chưa (bất kỳ đơn hàng nào)
     */
    boolean existsByVoucherIdAndUserId(Long voucherId, Long userId);
    
    /**
     * Lấy tất cả các lần sử dụng voucher của một user
     */
    List<VoucherUsage> findByVoucherIdAndUserId(Long voucherId, Long userId);
    
    /**
     * Lấy tất cả các lần sử dụng của một voucher
     */
    List<VoucherUsage> findByVoucherId(Long voucherId);
    
    /**
     * Đếm số lần voucher đã được sử dụng
     */
    long countByVoucherId(Long voucherId);
    
    /**
     * Đếm số lần user đã sử dụng voucher
     */
    long countByVoucherIdAndUserId(Long voucherId, Long userId);
    
    /**
     * Lấy voucher usage theo voucher, user và order
     */
    Optional<VoucherUsage> findByVoucherIdAndUserIdAndOrderId(Long voucherId, Long userId, Long orderId);
    
    /**
     * Lấy tất cả voucher usage của một đơn hàng
     */
    List<VoucherUsage> findByOrderId(Long orderId);
}


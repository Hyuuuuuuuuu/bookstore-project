package com.hutech.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * VoucherUsage - Theo dõi việc sử dụng voucher
 * 
 * Logic:
 * - Mỗi user chỉ được dùng voucher 1 lần cho 1 đơn hàng xác định
 * - Unique constraint đảm bảo: (voucher_id, user_id, order_id) là duy nhất
 * - Voucher có thể có usageLimit (giới hạn tổng số lần dùng) hoặc null (không giới hạn)
 * - Mỗi lần user dùng voucher cho đơn hàng mới sẽ tạo record mới trong bảng này
 */
@Entity
@Table(name = "voucher_usages", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"voucher_id", "user_id", "order_id"}, 
                            name = "uk_voucher_user_order")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "voucher_code", nullable = false, length = 20)
    private String voucherCode;

    @Column(name = "discount_amount", nullable = false)
    private Double discountAmount;

    @Column(name = "order_amount", nullable = false)
    private Double orderAmount;

    @Column(name = "used_at")
    @CreationTimestamp
    private LocalDateTime usedAt;

    @Column(name = "is_refunded", nullable = false)
    private Boolean isRefunded = false;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;
}


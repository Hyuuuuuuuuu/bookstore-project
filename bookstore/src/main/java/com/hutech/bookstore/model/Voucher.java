package com.hutech.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type;

    @Column(nullable = false)
    private Double value;

    @Column(name = "min_order_amount")
    private Double minOrderAmount = 0.0;

    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount;

    /**
     * Giới hạn số lần sử dụng voucher trên toàn hệ thống
     * - null: Không giới hạn lượt dùng
     * - Số nguyên dương: Giới hạn số lần sử dụng tối đa
     * Lưu ý: Mỗi user chỉ được dùng voucher 1 lần cho 1 đơn hàng xác định
     */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /**
     * Số lần voucher đã được sử dụng trên toàn hệ thống
     * Tự động tăng khi có user sử dụng voucher cho đơn hàng
     */
    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voucher_applicable_categories",
        joinColumns = @JoinColumn(name = "voucher_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> applicableCategories = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voucher_applicable_books",
        joinColumns = @JoinColumn(name = "voucher_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private List<Book> applicableBooks = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voucher_applicable_users",
        joinColumns = @JoinColumn(name = "voucher_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> applicableUsers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum VoucherType {
        PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING
    }
}


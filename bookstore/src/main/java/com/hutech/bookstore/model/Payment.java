package com.hutech.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "transaction_code", unique = true)
    private String transactionCode;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    @Column(length = 500)
    private String description;

    @Embedded
    private CustomerInfo customerInfo;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentMethod {
        COD, VNPAY, MOMO, ZALOPAY, PAYPAL, CREDIT_CARD, BANK_TRANSFER
    }

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        @Column(name = "ip_address", length = 50)
        private String ipAddress;

        @Column(name = "user_agent", length = 500)
        private String userAgent;
    }
}


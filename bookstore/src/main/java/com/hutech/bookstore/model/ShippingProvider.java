package com.hutech.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "base_fee", nullable = false)
    private Double baseFee;

    @Column(name = "estimated_time", nullable = false, length = 50)
    private String estimatedTime;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 500)
    private String description;

    @Embedded
    private ContactInfo contactInfo;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo {
        @Column(length = 20)
        private String phone;

        @Column(length = 100)
        private String email;

        @Column(length = 200)
        private String website;
    }
}


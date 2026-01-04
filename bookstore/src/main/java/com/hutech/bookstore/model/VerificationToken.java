package com.hutech.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;
}



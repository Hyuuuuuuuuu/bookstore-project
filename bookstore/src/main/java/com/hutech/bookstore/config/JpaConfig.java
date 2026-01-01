package com.hutech.bookstore.config;

import org.springframework.context.annotation.Configuration;

/**
 * JPA Configuration
 * 
 * Spring Boot tự động cấu hình JPA và Hibernate từ application.properties.
 * Không cần cấu hình thủ công EntityManagerFactory và TransactionManager.
 * 
 * Các cấu hình JPA được đặt trong application.properties:
 * - spring.jpa.hibernate.ddl-auto
 * - spring.jpa.show-sql
 * - spring.jpa.properties.hibernate.*
 */
@Configuration
public class JpaConfig {
    // Spring Boot tự động cấu hình JPA
    // Tất cả cấu hình được đặt trong application.properties
}


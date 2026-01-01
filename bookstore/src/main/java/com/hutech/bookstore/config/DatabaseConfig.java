package com.hutech.bookstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database Configuration
 * 
 * Cấu hình JPA và database connection.
 * Spring Boot tự động cấu hình DataSource từ application.properties.
 * 
 * Các cấu hình database được đặt trong application.properties:
 * - spring.datasource.url
 * - spring.datasource.username
 * - spring.datasource.password
 * - spring.jpa.hibernate.ddl-auto
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.hutech.bookstore.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // Spring Boot tự động cấu hình DataSource từ application.properties
    // Không cần tạo DataSource bean thủ công
}


package com.hutech.bookstore.config;

import org.springframework.context.annotation.Configuration;

/**
 * DataSource Configuration
 * 
 * Spring Boot tự động cấu hình DataSource và HikariCP connection pool
 * từ các properties trong application.properties.
 * 
 * Các cấu hình connection pool được đặt trong application.properties:
 * - spring.datasource.url
 * - spring.datasource.username
 * - spring.datasource.password
 * - spring.datasource.hikari.* (connection pool settings)
 * 
 * Không cần tạo DataSource bean thủ công vì Spring Boot đã tự động làm điều này.
 */
@Configuration
public class DataSourceConfig {
    // Spring Boot tự động cấu hình DataSource từ application.properties
    // HikariCP được sử dụng mặc định làm connection pool
}


package com.hutech.bookstore.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Exit Application Runner - Dừng ứng dụng sau khi seed/reset xong
 * Chỉ chạy khi app.seed.enabled=true (seed hoặc reset mode)
 * Chạy sau DataSeeder (Order 1) với Order 2
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
@Order(2) // Chạy sau DataSeeder (Order 1)
public class ExitApplicationRunner implements CommandLineRunner {

    @Value("${app.seed.clear-existing:false}")
    private boolean clearExisting;

    @Override
    public void run(String... args) throws Exception {
        // Đợi một chút để đảm bảo DataSeeder đã hoàn thành
        Thread.sleep(500);
        
        String mode = clearExisting ? "RESET" : "SEED";
        log.info("========================================");
        log.info("✅ {} mode completed successfully!", mode);
        log.info("========================================");
        log.info("Application will now exit...");
        log.info("To run the application normally, use: run-normal.bat");
        log.info("========================================");
        
        // Exit application
        System.exit(0);
    }
}


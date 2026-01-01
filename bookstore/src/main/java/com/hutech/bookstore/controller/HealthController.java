package com.hutech.bookstore.controller;

import com.hutech.bookstore.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("message", "Server is running");
        data.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Server is running"));
    }
}


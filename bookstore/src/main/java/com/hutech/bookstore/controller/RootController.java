package com.hutech.bookstore.controller;

import com.hutech.bookstore.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> root() {
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("message", "BookStore API Server");
        data.put("version", "1.0.0");
        data.put("timestamp", java.time.LocalDateTime.now());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/api/health");
        endpoints.put("auth", "/api/auth");
        endpoints.put("users", "/api/users");
        endpoints.put("books", "/api/books");
        endpoints.put("orders", "/api/orders");
        endpoints.put("cart", "/api/cart");
        endpoints.put("favorites", "/api/favorites");
        data.put("endpoints", endpoints);

        return ResponseEntity.ok(new ApiResponse<>(200, data, "BookStore API Server"));
    }
}


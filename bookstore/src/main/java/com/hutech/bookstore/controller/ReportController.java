package com.hutech.bookstore.controller;

import com.hutech.bookstore.service.ReportService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Lấy báo cáo thống kê
     * GET /api/reports/analytics?range=30days
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics(
            @RequestParam(required = false, defaultValue = "30days") String range) {
        
        Map<String, Object> data = reportService.getAnalytics(range);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Reports retrieved successfully"));
    }
}
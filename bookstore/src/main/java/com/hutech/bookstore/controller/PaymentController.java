package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.PaymentDTO;
import com.hutech.bookstore.service.PaymentService;
import com.hutech.bookstore.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * API Lấy danh sách thanh toán (Dành cho Admin)
     * Đã cập nhật: Thêm tham số search và status
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllPayments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search, // <-- Mới thêm
            @RequestParam(required = false) String status  // <-- Mới thêm
    ) {
        Map<String, Object> data = paymentService.getAllPayments(page, limit, search, status);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Payments retrieved successfully"));
    }

    /**
     * API Tạo URL thanh toán (Dành cho User khi Checkout)
     */
    @PostMapping("/create-url")
    public ResponseEntity<ApiResponse<PaymentDTO>> createPaymentUrl(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpServletRequest
    ) {
        // Lấy orderId và method từ body request
        Long orderId = Long.valueOf(request.get("orderId").toString());
        String method = request.get("method").toString();

        // Gọi service tạo URL (Service cũ của bạn xử lý logic này)
        PaymentDTO paymentDTO = paymentService.createPaymentUrl(orderId, method);
        
        return ResponseEntity.ok(new ApiResponse<>(200, paymentDTO, "Payment URL created"));
    }

    /**
     * API Lấy danh sách phương thức thanh toán hỗ trợ (Public)
     */
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<String>>> getPaymentMethods() {
        // Trả về danh sách cứng hoặc lấy từ DB tùy logic dự án
        List<String> methods = Arrays.asList("COD", "VNPAY", "MOMO"); 
        return ResponseEntity.ok(new ApiResponse<>(200, methods, "Payment methods retrieved"));
    }

    // --- CÁC API CALLBACK (VNPAY/MOMO) ---
    // Bạn có thể giữ nguyên các API callback cũ ở đây nếu có (ví dụ /vnpay-return)
}
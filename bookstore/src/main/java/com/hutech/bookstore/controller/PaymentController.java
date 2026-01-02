package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.PaymentResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.service.PaymentService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Lấy danh sách phương thức thanh toán
     * GET /api/payments/methods
     * Public endpoint
     */
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPaymentMethods() {
        List<Map<String, Object>> paymentMethods = List.of(
            Map.of(
                "id", "cod",
                "name", "Thanh toán khi nhận hàng (COD)",
                "description", "Thanh toán bằng tiền mặt khi nhận hàng",
                "icon", "cash",
                "enabled", true
            ),
            Map.of(
                "id", "vnpay",
                "name", "VNPay",
                "description", "Thanh toán qua VNPay",
                "icon", "vnpay",
                "enabled", true
            ),
            Map.of(
                "id", "momo",
                "name", "Ví MoMo",
                "description", "Thanh toán qua ví MoMo",
                "icon", "momo",
                "enabled", true
            ),
            Map.of(
                "id", "bank_transfer",
                "name", "Chuyển khoản ngân hàng",
                "description", "Chuyển khoản trực tiếp vào tài khoản ngân hàng",
                "icon", "bank",
                "enabled", true
            )
        );
        
        return ResponseEntity.ok(new ApiResponse<>(200, paymentMethods, "Payment methods retrieved successfully"));
    }

    /**
     * Tạo payment cho COD
     * POST /api/payments/cod
     * Yêu cầu: JWT Token (Admin only - có thể thay đổi sau)
     */
    @PostMapping("/cod")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> createCODPayment(
            @RequestBody Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        Double amount = request.get("amount") != null ? Double.valueOf(request.get("amount").toString()) : null;
        String description = request.get("description") != null ? request.get("description").toString() : null;
        
        PaymentResponseDTO payment = paymentService.createCODPayment(orderId, amount, description);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, payment, "COD payment created successfully"));
    }

    /**
     * Lấy danh sách payments (Admin only)
     * GET /api/payments
     * Yêu cầu: JWT Token (Admin)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method) {
        // TODO: Check admin role
        Map<String, Object> data = paymentService.getPayments(page, limit, status, method);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Payments retrieved successfully"));
    }

    /**
     * Lấy payment theo ID
     * GET /api/payments/:paymentId
     * Yêu cầu: JWT Token (Admin)
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentById(@PathVariable Long paymentId) {
        // TODO: Check admin role
        PaymentResponseDTO payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(new ApiResponse<>(200, payment, "Payment retrieved successfully"));
    }

    /**
     * Lấy payment theo transactionCode
     * GET /api/payments/transaction/:transactionCode
     * Yêu cầu: JWT Token (Admin)
     */
    @GetMapping("/transaction/{transactionCode}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentByTransactionCode(
            @PathVariable String transactionCode) {
        // TODO: Check admin role
        PaymentResponseDTO payment = paymentService.getPaymentByTransactionCode(transactionCode);
        return ResponseEntity.ok(new ApiResponse<>(200, payment, "Payment retrieved successfully"));
    }
}


package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.PaymentResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.service.PaymentService;
import com.hutech.bookstore.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    
    @Value("${frontend.url}")
    private String frontendUrl;

    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPaymentMethods() {
        List<Map<String, Object>> paymentMethods = List.of(
            Map.of("id", "cod", "name", "Thanh toán khi nhận hàng (COD)", "description", "Thanh toán bằng tiền mặt khi nhận hàng", "icon", "cash", "enabled", true),
            Map.of("id", "vnpay", "name", "VNPay", "description", "Thanh toán qua VNPay", "icon", "vnpay", "enabled", true),
            Map.of("id", "momo", "name", "Ví MoMo", "description", "Thanh toán qua ví MoMo", "icon", "momo", "enabled", true),
            Map.of("id", "bank_transfer", "name", "Chuyển khoản ngân hàng", "description", "Chuyển khoản trực tiếp vào tài khoản ngân hàng", "icon", "bank", "enabled", true)
        );
        return ResponseEntity.ok(new ApiResponse<>(200, paymentMethods, "Payment methods retrieved successfully"));
    }

    @PostMapping("/cod")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> createCODPayment(@RequestBody Map<String, Object> request) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        Double amount = request.get("amount") != null ? Double.valueOf(request.get("amount").toString()) : null;
        String description = request.get("description") != null ? request.get("description").toString() : null;
        
        PaymentResponseDTO payment = paymentService.createCODPayment(orderId, amount, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, payment, "COD payment created successfully"));
    }

    @PostMapping("/create-url")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentUrl(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        Long orderId = Long.valueOf(request.get("orderId").toString());
        String method = request.get("method").toString();
        Double amount = request.get("amount") != null ? Double.valueOf(request.get("amount").toString()) : null;
        String ipAddress = servletRequest.getRemoteAddr();

        Map<String, Object> result = paymentService.createOnlinePaymentUrl(orderId, method, amount, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, result, "Payment URL created successfully"));
    }

    @GetMapping("/callback/{gateway}")
    public void handleCallback(@PathVariable String gateway, @RequestParam String status, @RequestParam String orderId, @RequestParam String transId, HttpServletResponse response) throws IOException {
        boolean isSuccess = "success".equalsIgnoreCase(status);
        paymentService.handlePaymentCallback(transId, isSuccess, "Callback from " + gateway);
        if (isSuccess) {
            response.sendRedirect(frontendUrl + "/payment/success?orderId=" + orderId);
        } else {
            response.sendRedirect(frontendUrl + "/payment/failed?orderId=" + orderId);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> updatePaymentStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        if (status == null) throw new AppException("Status is required", 400);
        PaymentResponseDTO payment = paymentService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(200, payment, "Payment status updated successfully"));
    }

    /**
     * Lấy danh sách payments (Admin only) - Đã thêm param search
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search, // Thêm search
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method) {
        
        // TODO: Check admin role
        Map<String, Object> data = paymentService.getPayments(page, limit, search, status, method);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Payments retrieved successfully"));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentById(@PathVariable Long paymentId) {
        PaymentResponseDTO payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(new ApiResponse<>(200, payment, "Payment retrieved successfully"));
    }

    @GetMapping("/transaction/{transactionCode}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentByTransactionCode(@PathVariable String transactionCode) {
        PaymentResponseDTO payment = paymentService.getPaymentByTransactionCode(transactionCode);
        return ResponseEntity.ok(new ApiResponse<>(200, payment, "Payment retrieved successfully"));
    }
}
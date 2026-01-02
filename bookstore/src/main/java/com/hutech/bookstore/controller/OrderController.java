package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.CreateOrderRequest;
import com.hutech.bookstore.dto.OrderResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.service.OrderService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng mới
     * POST /api/orders
     * Yêu cầu: JWT Token
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(@RequestBody CreateOrderRequest request) {
        User user = getCurrentUser();
        OrderResponseDTO order = orderService.createOrder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, order, "Order created successfully"));
    }

    /**
     * Lấy danh sách đơn hàng của user hiện tại
     * GET /api/orders/my-orders
     * Yêu cầu: JWT Token
     */
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status) {
        User user = getCurrentUser();
        Map<String, Object> data = orderService.getUserOrders(user, page, limit, status);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Orders retrieved successfully"));
    }

    /**
     * Lấy tất cả đơn hàng (Admin only)
     * GET /api/orders
     * Yêu cầu: JWT Token (Admin)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {
        // TODO: Check admin role
        Map<String, Object> data = orderService.getAllOrders(page, limit, status, userId);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "All orders retrieved successfully"));
    }

    /**
     * Lấy chi tiết đơn hàng
     * GET /api/orders/:orderId
     * Yêu cầu: JWT Token
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable Long orderId) {
        User user = getCurrentUser();
        boolean isAdmin = user.getRole().getName().equalsIgnoreCase("admin") || 
                         user.getRole().getName().equalsIgnoreCase("staff");
        OrderResponseDTO order = orderService.getOrderById(user, orderId, isAdmin);
        return ResponseEntity.ok(new ApiResponse<>(200, order, "Order retrieved successfully"));
    }

    /**
     * Hủy đơn hàng
     * PATCH /api/orders/:orderId/cancel
     * Yêu cầu: JWT Token
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(@PathVariable Long orderId) {
        User user = getCurrentUser();
        OrderResponseDTO order = orderService.cancelOrder(user, orderId);
        return ResponseEntity.ok(new ApiResponse<>(200, order, "Order cancelled successfully"));
    }

    /**
     * Cập nhật trạng thái đơn hàng (Admin only)
     * PATCH /api/orders/admin/:orderId/status
     * Yêu cầu: JWT Token (Admin)
     */
    @PatchMapping("/admin/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        // TODO: Check admin role
        String status = request.get("status");
        if (status == null || status.trim().isEmpty()) {
            throw new AppException("Status is required", 400);
        }
        
        OrderResponseDTO order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(new ApiResponse<>(200, order, "Order status updated successfully"));
    }

    /**
     * Helper method để lấy user hiện tại từ SecurityContext
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || 
            authentication.getPrincipal() == null || 
            !(authentication.getPrincipal() instanceof User)) {
            throw new AppException("Authentication required. Please provide a valid JWT token.", 401);
        }
        return (User) authentication.getPrincipal();
    }
}


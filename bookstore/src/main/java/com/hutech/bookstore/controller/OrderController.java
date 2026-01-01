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

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng mới
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
     * Lấy tất cả đơn hàng (Admin only) - Đã cập nhật hỗ trợ tìm kiếm
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search, // Thêm Search
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {
        
        // Gọi hàm getAllOrdersWithSearch mới trong Service
        Map<String, Object> data = orderService.getAllOrdersWithSearch(page, limit, search, status, userId);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "All orders retrieved successfully"));
    }

    /**
     * Lấy chi tiết đơn hàng
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable Long orderId) {
        User user = getCurrentUser();
        // Kiểm tra role để xác định quyền xem
        boolean isAdmin = user.getRole().getName().equalsIgnoreCase("admin") || 
                         user.getRole().getName().equalsIgnoreCase("staff");
        
        OrderResponseDTO order = orderService.getOrderById(user, orderId, isAdmin);
        return ResponseEntity.ok(new ApiResponse<>(200, order, "Order retrieved successfully"));
    }

    /**
     * Hủy đơn hàng
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(@PathVariable Long orderId) {
        User user = getCurrentUser();
        OrderResponseDTO order = orderService.cancelOrder(user, orderId);
        return ResponseEntity.ok(new ApiResponse<>(200, order, "Order cancelled successfully"));
    }

    /**
     * Cập nhật trạng thái đơn hàng (Admin only)
     * URL: /api/orders/admin/{orderId}/status
     */
    @PatchMapping("/admin/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        
        String status = request.get("status");
        
        // Validate input
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
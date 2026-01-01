package com.hutech.bookstore.controller;

import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.service.CartService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Lấy giỏ hàng của user hiện tại
     * GET /api/cart
     * Yêu cầu: JWT Token
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCart() {
        User user = getCurrentUser();
        Map<String, Object> data = cartService.getUserCart(user);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Cart retrieved successfully"));
    }

    /**
     * Thêm sách vào giỏ hàng
     * POST /api/cart/:bookId
     * Yêu cầu: JWT Token
     * Body: { "quantity": 1 } (optional, default: 1)
     */
    @PostMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToCart(
            @PathVariable Long bookId,
            @RequestBody(required = false) Map<String, Integer> request) {
        User user = getCurrentUser();
        Integer quantity = (request != null && request.containsKey("quantity")) 
            ? request.get("quantity") 
            : 1;
        
        Map<String, Object> data = cartService.addToCart(user, bookId, quantity);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Book added to cart successfully"));
    }

    /**
     * Cập nhật số lượng sách trong giỏ hàng
     * PUT /api/cart/:bookId
     * Yêu cầu: JWT Token
     * Body: { "quantity": 2 }
     */
    @PutMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCartItem(
            @PathVariable Long bookId,
            @RequestBody Map<String, Integer> request) {
        User user = getCurrentUser();
        Integer quantity = request.get("quantity");
        
        if (quantity == null) {
            throw new AppException("Quantity is required", 400);
        }
        
        Map<String, Object> data = cartService.updateCartItem(user, bookId, quantity);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Cart item updated successfully"));
    }

    /**
     * Xóa sách khỏi giỏ hàng
     * DELETE /api/cart/:bookId
     * Yêu cầu: JWT Token
     */
    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFromCart(@PathVariable Long bookId) {
        User user = getCurrentUser();
        Map<String, Object> data = cartService.removeFromCart(user, bookId);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Book removed from cart successfully"));
    }

    /**
     * Xóa tất cả sách khỏi giỏ hàng
     * DELETE /api/cart
     * Yêu cầu: JWT Token
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearCart() {
        User user = getCurrentUser();
        Map<String, Object> data = cartService.clearCart(user);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Cart cleared successfully"));
    }

    /**
     * Kiểm tra sách có trong giỏ hàng không
     * GET /api/cart/check/:bookId
     * Yêu cầu: JWT Token
     */
    @GetMapping("/check/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCartItem(@PathVariable Long bookId) {
        User user = getCurrentUser();
        Map<String, Object> data = cartService.checkCartItem(user, bookId);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Cart item status retrieved successfully"));
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


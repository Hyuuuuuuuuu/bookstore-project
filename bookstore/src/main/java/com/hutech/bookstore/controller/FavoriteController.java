package com.hutech.bookstore.controller;

import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.service.FavoriteService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * Thêm sách vào danh sách yêu thích của user hiện tại
     * POST /api/favorites/:bookId
     * Yêu cầu: JWT Token trong header Authorization: Bearer <token>
     */
    @PostMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToFavorites(@PathVariable Long bookId) {
        User user = getCurrentUser();
        var favoriteDTO = favoriteService.addToFavorites(user, bookId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("favorite", favoriteDTO);
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Book added to favorites successfully"));
    }

    /**
     * Xóa sách khỏi danh sách yêu thích của user hiện tại
     * DELETE /api/favorites/:bookId
     * Yêu cầu: JWT Token trong header Authorization: Bearer <token>
     */
    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFromFavorites(@PathVariable Long bookId) {
        User user = getCurrentUser();
        var favoriteDTO = favoriteService.removeFromFavorites(user, bookId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("favorite", favoriteDTO);
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Book removed from favorites successfully"));
    }

    /**
     * Lấy danh sách sách yêu thích của user hiện tại
     * GET /api/favorites
     * Yêu cầu: JWT Token trong header Authorization: Bearer <token>
     * Chỉ trả về favorites của user đang đăng nhập
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFavorites() {
        User user = getCurrentUser();
        Map<String, Object> data = favoriteService.getUserFavorites(user);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Favorites retrieved successfully"));
    }

    /**
     * Kiểm tra sách có trong favorites của user hiện tại không
     * GET /api/favorites/check/:bookId
     * Yêu cầu: JWT Token trong header Authorization: Bearer <token>
     */
    @GetMapping("/check/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkFavorite(@PathVariable Long bookId) {
        User user = getCurrentUser();
        boolean isFavorite = favoriteService.checkFavorite(user, bookId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("isFavorite", isFavorite);
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Favorite status retrieved successfully"));
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


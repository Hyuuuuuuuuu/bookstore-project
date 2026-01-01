package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.AddressResponseDTO;
import com.hutech.bookstore.dto.CreateAddressRequest;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.service.AddressService;
import com.hutech.bookstore.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * Helper method để lấy user hiện tại từ SecurityContext
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new com.hutech.bookstore.exception.AppException("User not authenticated", 401);
        }
        return (User) authentication.getPrincipal();
    }

    /**
     * Lấy danh sách địa chỉ của user hiện tại
     * GET /api/addresses
     * Yêu cầu: JWT Token
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserAddresses() {
        User user = getCurrentUser();
        Map<String, Object> data = addressService.getUserAddresses(user);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Addresses retrieved successfully"));
    }

    /**
     * Lấy địa chỉ mặc định của user hiện tại
     * GET /api/addresses/default
     * Yêu cầu: JWT Token
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> getDefaultAddress() {
        User user = getCurrentUser();
        AddressResponseDTO address = addressService.getDefaultAddress(user);
        if (address == null) {
            return ResponseEntity.ok(new ApiResponse<>(200, null, "No default address found"));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, address, "Default address retrieved successfully"));
    }

    /**
     * Tạo địa chỉ mới
     * POST /api/addresses
     * Yêu cầu: JWT Token
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        User user = getCurrentUser();
        AddressResponseDTO address = addressService.createAddress(user, request);
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("address", address);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, data, "Address created successfully"));
    }

    /**
     * Cập nhật địa chỉ
     * PUT /api/addresses/:id
     * Yêu cầu: JWT Token
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody CreateAddressRequest request) {
        User user = getCurrentUser();
        AddressResponseDTO address = addressService.updateAddress(user, id, request);
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("address", address);
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Address updated successfully"));
    }

    /**
     * Xóa địa chỉ
     * DELETE /api/addresses/:id
     * Yêu cầu: JWT Token
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAddress(@PathVariable Long id) {
        User user = getCurrentUser();
        addressService.deleteAddress(user, id);
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("message", "Address deleted successfully");
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Address deleted successfully"));
    }

    /**
     * Đặt địa chỉ làm mặc định
     * PUT /api/addresses/:id/default
     * Yêu cầu: JWT Token
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setDefaultAddress(@PathVariable Long id) {
        User user = getCurrentUser();
        AddressResponseDTO address = addressService.setDefaultAddress(user, id);
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("address", address);
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Default address set successfully"));
    }
}


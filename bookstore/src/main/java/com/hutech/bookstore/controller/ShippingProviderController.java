package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.ShippingProviderResponseDTO;
import com.hutech.bookstore.service.ShippingProviderService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipping-providers")
@RequiredArgsConstructor
public class ShippingProviderController {

    private final ShippingProviderService shippingProviderService;

    /**
     * Lấy danh sách đơn vị vận chuyển đang hoạt động (Public)
     * GET /api/shipping-providers/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveShippingProviders() {
        Map<String, Object> data = shippingProviderService.getActiveShippingProviders();
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Active shipping providers retrieved successfully"));
    }

    /**
     * Lấy tất cả đơn vị vận chuyển đang hoạt động (Public)
     * GET /api/shipping-providers
     * Trả về chỉ các đơn vị vận chuyển đang active và chưa bị xóa
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllShippingProviders() {
        Map<String, Object> data = shippingProviderService.getActiveShippingProviders();
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Shipping providers retrieved successfully"));
    }

    /**
     * Lấy đơn vị vận chuyển theo ID
     * GET /api/shipping-providers/:id
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> getShippingProviderById(@PathVariable Long id) {
        ShippingProviderResponseDTO provider = shippingProviderService.getShippingProviderById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, provider, "Shipping provider retrieved successfully"));
    }

    /**
     * Lấy đơn vị vận chuyển theo code
     * GET /api/shipping-providers/code/:code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> getShippingProviderByCode(@PathVariable String code) {
        ShippingProviderResponseDTO provider = shippingProviderService.getShippingProviderByCode(code);
        return ResponseEntity.ok(new ApiResponse<>(200, provider, "Shipping provider retrieved successfully"));
    }
}


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
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllShippingProviders() {
        // Return all non-deleted providers (admin/public endpoint may use authorization if needed)
        Map<String, Object> data = shippingProviderService.getAllShippingProviders();
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

    /**
     * Cập nhật đơn vị vận chuyển (Admin)
     * PUT /api/shipping-providers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> updateShippingProvider(
            @PathVariable Long id,
            @RequestBody com.hutech.bookstore.dto.ShippingProviderRequestDTO request) {
        ShippingProviderResponseDTO updated = shippingProviderService.updateShippingProvider(id, request);
        return ResponseEntity.ok(new ApiResponse<>(200, updated, "Shipping provider updated successfully"));
    }

    /**
     * Xóa đơn vị vận chuyển (Admin) - soft delete
     * DELETE /api/shipping-providers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteShippingProvider(@PathVariable Long id) {
        shippingProviderService.deleteShippingProvider(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "Shipping provider deleted successfully"));
    }

    /**
     * Tạo mới đơn vị vận chuyển (Admin)
     * POST /api/shipping-providers
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> createShippingProvider(@RequestBody com.hutech.bookstore.dto.ShippingProviderRequestDTO request) {
        ShippingProviderResponseDTO created = shippingProviderService.createShippingProvider(request);
        return ResponseEntity.status(201).body(new ApiResponse<>(201, created, "Shipping provider created successfully"));
    }

}


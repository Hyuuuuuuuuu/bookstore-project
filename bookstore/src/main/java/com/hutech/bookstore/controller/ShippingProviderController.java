package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.ShippingProviderResponseDTO;
import com.hutech.bookstore.dto.ShippingProviderRequestDTO;
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
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveShippingProviders() {
        Map<String, Object> data = shippingProviderService.getActiveShippingProviders();
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Active shipping providers retrieved successfully"));
    }

    /**
     * Lấy tất cả đơn vị vận chuyển (Admin) - CÓ SEARCH/FILTER/SORT
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllShippingProviders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status, // active/inactive/all
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        
        Map<String, Object> data = shippingProviderService.getAllShippingProviders(search, status, sortBy, sortOrder);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Shipping providers retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> getShippingProviderById(@PathVariable Long id) {
        ShippingProviderResponseDTO provider = shippingProviderService.getShippingProviderById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, provider, "Shipping provider retrieved successfully"));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> getShippingProviderByCode(@PathVariable String code) {
        ShippingProviderResponseDTO provider = shippingProviderService.getShippingProviderByCode(code);
        return ResponseEntity.ok(new ApiResponse<>(200, provider, "Shipping provider retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> updateShippingProvider(
            @PathVariable Long id,
            @RequestBody ShippingProviderRequestDTO request) {
        ShippingProviderResponseDTO updated = shippingProviderService.updateShippingProvider(id, request);
        return ResponseEntity.ok(new ApiResponse<>(200, updated, "Shipping provider updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteShippingProvider(@PathVariable Long id) {
        shippingProviderService.deleteShippingProvider(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "Shipping provider deleted successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShippingProviderResponseDTO>> createShippingProvider(@RequestBody ShippingProviderRequestDTO request) {
        ShippingProviderResponseDTO created = shippingProviderService.createShippingProvider(request);
        return ResponseEntity.status(201).body(new ApiResponse<>(201, created, "Shipping provider created successfully"));
    }
}
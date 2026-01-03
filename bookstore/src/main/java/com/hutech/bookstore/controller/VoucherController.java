package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.VoucherResponseDTO;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.service.VoucherService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailableVouchers(
            @RequestParam(required = false) Double orderAmount,
            @RequestParam(required = false) String categoryIds,
            @RequestParam(required = false) String bookIds) {
        
        List<Long> categoryIdList = null;
        if (categoryIds != null && !categoryIds.trim().isEmpty()) {
            try {
                categoryIdList = new ArrayList<>();
                for (String id : categoryIds.split(",")) categoryIdList.add(Long.parseLong(id.trim()));
            } catch (Exception ignored) {}
        }
        
        List<Long> bookIdList = null;
        if (bookIds != null && !bookIds.trim().isEmpty()) {
            try {
                bookIdList = new ArrayList<>();
                for (String id : bookIds.split(",")) bookIdList.add(Long.parseLong(id.trim()));
            } catch (Exception ignored) {}
        }
        
        Long userId = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                userId = ((User) authentication.getPrincipal()).getId();
            }
        } catch (Exception ignored) {}
        
        Map<String, Object> data = voucherService.getAvailableVouchers(orderAmount, categoryIdList, bookIdList, userId);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Available vouchers retrieved successfully"));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllVouchersForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        Map<String, Object> data = voucherService.getAllVouchersForAdmin(status, type, search);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Vouchers retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllVouchers(
            @RequestParam(required = false, defaultValue = "false") Boolean validOnly) {
        Map<String, Object> data;
        if (Boolean.TRUE.equals(validOnly)) {
            data = voucherService.getAllValidVouchers();
        } else {
            data = voucherService.getAllVouchers();
        }
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Vouchers retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponseDTO>> getVoucher(@PathVariable Long id) {
        VoucherResponseDTO voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, voucher, "Voucher retrieved successfully"));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<VoucherResponseDTO>> getVoucherByCode(@PathVariable String code) {
        VoucherResponseDTO voucher = voucherService.getVoucherByCode(code);
        return ResponseEntity.ok(new ApiResponse<>(200, voucher, "Voucher retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponseDTO>> updateVoucher(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        VoucherResponseDTO updated = voucherService.updateVoucher(id, payload);
        return ResponseEntity.ok(new ApiResponse<>(200, updated, "Voucher updated successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponseDTO>> createVoucher(@RequestBody Map<String, Object> payload) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                payload.put("createdById", user.getId());
            }
        } catch (Exception ignored) {}

        VoucherResponseDTO created = voucherService.createVoucher(payload);
        return ResponseEntity.status(201).body(new ApiResponse<>(201, created, "Voucher created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "Voucher deleted successfully"));
    }
}
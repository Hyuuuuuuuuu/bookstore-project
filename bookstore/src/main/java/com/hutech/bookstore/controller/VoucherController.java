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

    /**
     * Lấy danh sách voucher có thể áp dụng cho đơn hàng
     * GET /api/vouchers/available
     * Query Parameters:
     *   - orderAmount: Tổng tiền đơn hàng (optional)
     *   - categoryIds: Danh sách category IDs, phân cách bằng dấu phẩy (optional)
     *   - bookIds: Danh sách book IDs, phân cách bằng dấu phẩy (optional)
     * Yêu cầu: JWT Token (để lấy userId và kiểm tra đã dùng voucher chưa)
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailableVouchers(
            @RequestParam(required = false) Double orderAmount,
            @RequestParam(required = false) String categoryIds,
            @RequestParam(required = false) String bookIds) {
        
        // Parse categoryIds từ string sang List<Long>
        List<Long> categoryIdList = null;
        if (categoryIds != null && !categoryIds.trim().isEmpty()) {
            try {
                categoryIdList = new ArrayList<>();
                String[] ids = categoryIds.split(",");
                for (String id : ids) {
                    categoryIdList.add(Long.parseLong(id.trim()));
                }
            } catch (NumberFormatException e) {
                // Ignore invalid IDs
            }
        }
        
        // Parse bookIds từ string sang List<Long>
        List<Long> bookIdList = null;
        if (bookIds != null && !bookIds.trim().isEmpty()) {
            try {
                bookIdList = new ArrayList<>();
                String[] ids = bookIds.split(",");
                for (String id : ids) {
                    bookIdList.add(Long.parseLong(id.trim()));
                }
            } catch (NumberFormatException e) {
                // Ignore invalid IDs
            }
        }
        
        // Lấy userId từ authentication (nếu có)
        Long userId = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                userId = user.getId();
            }
        } catch (Exception e) {
            // Nếu không có authentication, userId = null (vẫn có thể lấy voucher public)
        }
        
        Map<String, Object> data = voucherService.getAvailableVouchers(
            orderAmount, categoryIdList, bookIdList, userId
        );
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Available vouchers retrieved successfully"));
    }

    /**
     * Lấy tất cả voucher (active và chưa bị xóa, không filter theo thời gian)
     * GET /api/vouchers
     * Query Parameters:
     *   - validOnly: true để chỉ lấy voucher còn hợp lệ (validFrom <= now AND validTo >= now), false để lấy tất cả (default: false)
     * Public endpoint
     */
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

    /**
     * Lấy voucher theo code
     * GET /api/vouchers/code/:code
     * Public endpoint
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<VoucherResponseDTO>> getVoucherByCode(@PathVariable String code) {
        VoucherResponseDTO voucher = voucherService.getVoucherByCode(code);
        return ResponseEntity.ok(new ApiResponse<>(200, voucher, "Voucher retrieved successfully"));
    }
}


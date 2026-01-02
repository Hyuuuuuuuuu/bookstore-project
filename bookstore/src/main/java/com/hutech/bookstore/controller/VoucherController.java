package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.VoucherDTO;
import com.hutech.bookstore.service.VoucherService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    /**
     * 1. Lấy danh sách Voucher (Hỗ trợ Tìm kiếm & Bộ lọc)
     * URL: GET /api/vouchers?page=1&limit=10&search=CODE&status=true&type=PERCENTAGE
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllVouchers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "all") String type
    ) {
        Map<String, Object> data = voucherService.getAllVouchers(page, limit, search, status, type);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Vouchers retrieved successfully"));
    }

    /**
     * 2. Lấy chi tiết 1 Voucher theo ID (Dùng khi bấm nút "Sửa")
     * URL: GET /api/vouchers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherDTO>> getVoucherById(@PathVariable Long id) {
        VoucherDTO voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, voucher, "Voucher detail retrieved"));
    }

    /**
     * 3. Tạo Voucher mới
     * URL: POST /api/vouchers
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VoucherDTO>> createVoucher(@RequestBody VoucherDTO voucherDTO) {
        VoucherDTO createdVoucher = voucherService.createVoucher(voucherDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, createdVoucher, "Voucher created successfully"));
    }

    /**
     * 4. Cập nhật Voucher
     * URL: PUT /api/vouchers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherDTO>> updateVoucher(
            @PathVariable Long id,
            @RequestBody VoucherDTO voucherDTO) {
        VoucherDTO updatedVoucher = voucherService.updateVoucher(id, voucherDTO);
        return ResponseEntity.ok(new ApiResponse<>(200, updatedVoucher, "Voucher updated successfully"));
    }

    /**
     * 5. Xóa Voucher
     * URL: DELETE /api/vouchers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "Voucher deleted successfully"));
    }
}
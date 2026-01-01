package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.VoucherResponseDTO;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.model.Voucher;
import com.hutech.bookstore.repository.VoucherRepository;
import com.hutech.bookstore.repository.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    /**
     * Lấy danh sách voucher có thể áp dụng cho đơn hàng
     * 
     * @param orderAmount Tổng tiền đơn hàng (để filter theo minOrderAmount)
     * @param categoryIds Danh sách category IDs trong đơn hàng (để filter applicable categories)
     * @param bookIds Danh sách book IDs trong đơn hàng (để filter applicable books)
     * @param userId User ID (để filter applicable users và kiểm tra đã dùng chưa)
     * @return Danh sách voucher có thể áp dụng
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableVouchers(
            Double orderAmount,
            List<Long> categoryIds,
            List<Long> bookIds,
            Long userId) {
        
        LocalDateTime now = LocalDateTime.now();
        
        // Lấy tất cả voucher hợp lệ (active, chưa hết hạn, chưa đạt limit)
        List<Voucher> validVouchers = voucherRepository.findValidVouchers(now);
        
        // Fetch từng collection riêng biệt để tránh MultipleBagFetchException
        // Hibernate sẽ tự động fetch khi truy cập collection trong transaction
        validVouchers.forEach(voucher -> {
            // Fetch applicableCategories
            if (voucher.getApplicableCategories() != null) {
                voucher.getApplicableCategories().size();
            }
            // Fetch applicableBooks (sau khi fetch categories xong)
            if (voucher.getApplicableBooks() != null) {
                voucher.getApplicableBooks().size();
            }
            // Fetch applicableUsers (sau khi fetch books xong)
            if (voucher.getApplicableUsers() != null) {
                voucher.getApplicableUsers().size();
            }
        });
        
        // Filter theo điều kiện
        List<VoucherResponseDTO> availableVouchers = validVouchers.stream()
            .filter(voucher -> {
                // 1. Kiểm tra minOrderAmount
                if (orderAmount != null && voucher.getMinOrderAmount() != null) {
                    if (orderAmount < voucher.getMinOrderAmount()) {
                        return false;
                    }
                }
                
                // 2. Kiểm tra applicable categories (nếu có)
                if (voucher.getApplicableCategories() != null && !voucher.getApplicableCategories().isEmpty()) {
                    if (categoryIds == null || categoryIds.isEmpty()) {
                        return false; // Voucher chỉ áp dụng cho categories cụ thể nhưng đơn hàng không có category nào
                    }
                    boolean hasMatchingCategory = voucher.getApplicableCategories().stream()
                        .anyMatch(cat -> categoryIds.contains(cat.getId()));
                    if (!hasMatchingCategory) {
                        return false;
                    }
                }
                
                // 3. Kiểm tra applicable books (nếu có)
                if (voucher.getApplicableBooks() != null && !voucher.getApplicableBooks().isEmpty()) {
                    if (bookIds == null || bookIds.isEmpty()) {
                        return false; // Voucher chỉ áp dụng cho books cụ thể nhưng đơn hàng không có book nào
                    }
                    boolean hasMatchingBook = voucher.getApplicableBooks().stream()
                        .anyMatch(book -> bookIds.contains(book.getId()));
                    if (!hasMatchingBook) {
                        return false;
                    }
                }
                
                // 4. Kiểm tra applicable users (nếu có)
                if (voucher.getApplicableUsers() != null && !voucher.getApplicableUsers().isEmpty()) {
                    if (userId == null) {
                        return false; // Voucher chỉ áp dụng cho users cụ thể nhưng không có userId
                    }
                    boolean isApplicableToUser = voucher.getApplicableUsers().stream()
                        .anyMatch(user -> user.getId().equals(userId));
                    if (!isApplicableToUser) {
                        return false;
                    }
                }
                
                // 5. Kiểm tra user đã dùng voucher này chưa (nếu có userId)
                if (userId != null) {
                    boolean hasUsed = voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), userId);
                    if (hasUsed) {
                        return false; // User đã dùng voucher này rồi
                    }
                }
                
                return true;
            })
            .map(VoucherResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", availableVouchers);
        data.put("total", availableVouchers.size());
        
        return data;
    }

    /**
     * Lấy tất cả voucher hợp lệ (active, chưa hết hạn, chưa đạt limit)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllValidVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> validVouchers = voucherRepository.findValidVouchers(now);
        
        // Fetch từng collection riêng biệt để tránh MultipleBagFetchException
        // Hibernate sẽ tự động fetch khi truy cập collection trong transaction
        validVouchers.forEach(voucher -> {
            // Fetch applicableCategories
            if (voucher.getApplicableCategories() != null) {
                voucher.getApplicableCategories().size();
            }
            // Fetch applicableBooks (sau khi fetch categories xong)
            if (voucher.getApplicableBooks() != null) {
                voucher.getApplicableBooks().size();
            }
            // Fetch applicableUsers (sau khi fetch books xong)
            if (voucher.getApplicableUsers() != null) {
                voucher.getApplicableUsers().size();
            }
        });
        
        List<VoucherResponseDTO> voucherDTOs = validVouchers.stream()
            .map(VoucherResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", voucherDTOs);
        data.put("total", voucherDTOs.size());
        
        return data;
    }

    /**
     * Lấy tất cả voucher (active và chưa bị xóa, không filter theo thời gian)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllVouchers() {
        List<Voucher> vouchers = voucherRepository.findByIsActiveTrueAndIsDeletedFalse();
        
        // Fetch từng collection riêng biệt để tránh MultipleBagFetchException
        vouchers.forEach(voucher -> {
            if (voucher.getApplicableCategories() != null) {
                voucher.getApplicableCategories().size();
            }
            if (voucher.getApplicableBooks() != null) {
                voucher.getApplicableBooks().size();
            }
            if (voucher.getApplicableUsers() != null) {
                voucher.getApplicableUsers().size();
            }
        });
        
        List<VoucherResponseDTO> voucherDTOs = vouchers.stream()
            .map(VoucherResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", voucherDTOs);
        data.put("total", voucherDTOs.size());
        
        return data;
    }

    /**
     * Lấy voucher theo code
     */
    @Transactional(readOnly = true)
    public VoucherResponseDTO getVoucherByCode(String code) {
        Voucher voucher = voucherRepository.findByCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new com.hutech.bookstore.exception.AppException("Voucher not found", 404));
        
        // Fetch từng collection riêng biệt để tránh MultipleBagFetchException
        // Hibernate sẽ tự động fetch khi truy cập collection trong transaction
        if (voucher.getApplicableCategories() != null) {
            voucher.getApplicableCategories().size();
        }
        if (voucher.getApplicableBooks() != null) {
            voucher.getApplicableBooks().size();
        }
        if (voucher.getApplicableUsers() != null) {
            voucher.getApplicableUsers().size();
        }
        
        return VoucherResponseDTO.fromEntity(voucher);
    }
}


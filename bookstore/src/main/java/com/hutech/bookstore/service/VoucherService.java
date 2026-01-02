package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.VoucherResponseDTO;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.model.Voucher;
import com.hutech.bookstore.repository.VoucherRepository;
import com.hutech.bookstore.repository.VoucherUsageRepository;
import com.hutech.bookstore.repository.UserRepository;
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
    private final UserRepository userRepository;

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
     * Lấy tất cả voucher cho admin (với filter)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllVouchersForAdmin(String status, String type, String search) {
        List<Voucher> vouchers = voucherRepository.findByIsDeletedFalse();

        // Filter theo status
        if (status != null && !status.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            vouchers = vouchers.stream()
                .filter(voucher -> {
                    String computedStatus = computeVoucherStatus(voucher, now);
                    return computedStatus.equals(status);
                })
                .collect(Collectors.toList());
        }

        // Filter theo type
        if (type != null && !type.isEmpty()) {
            vouchers = vouchers.stream()
                .filter(voucher -> voucher.getType().name().toLowerCase().equals(type.toLowerCase()))
                .collect(Collectors.toList());
        }

        // Filter theo search (code hoặc name)
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            vouchers = vouchers.stream()
                .filter(voucher ->
                    voucher.getCode().toLowerCase().contains(searchLower) ||
                    voucher.getName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }

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
     * Tính toán trạng thái voucher
     */
    private String computeVoucherStatus(Voucher voucher, LocalDateTime now) {
        // Nếu admin tắt thì inactive
        if (!voucher.getIsActive()) {
            return "inactive";
        }

        // Nếu hết hạn thì expired
        if (voucher.getValidTo().isBefore(now)) {
            return "expired";
        }

        // Nếu hết lượt dùng thì expired
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return "expired";
        }

        // Còn lại là active
        return "active";
    }

    /**
     * Lấy voucher theo ID
     */
    @Transactional(readOnly = true)
    public VoucherResponseDTO getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
            .filter(v -> !v.getIsDeleted())
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

    /**
     * Cập nhật voucher (admin)
     */
    @Transactional
    public VoucherResponseDTO updateVoucher(Long id, Map<String, Object> updates) {
        Voucher voucher = voucherRepository.findById(id)
            .filter(v -> !v.getIsDeleted())
            .orElseThrow(() -> new com.hutech.bookstore.exception.AppException("Voucher not found", 404));

        // Update simple fields if present
        if (updates.containsKey("code")) {
            voucher.setCode(String.valueOf(updates.get("code")).trim());
        }
        if (updates.containsKey("name")) {
            voucher.setName(String.valueOf(updates.get("name")).trim());
        }
        if (updates.containsKey("description")) {
            voucher.setDescription(updates.get("description") != null ? String.valueOf(updates.get("description")).trim() : null);
        }
        if (updates.containsKey("type")) {
            String typeStr = String.valueOf(updates.get("type")).toUpperCase();
            try {
                voucher.setType(Voucher.VoucherType.valueOf(typeStr));
            } catch (IllegalArgumentException ignored) {}
        }
        if (updates.containsKey("value")) {
            try {
                voucher.setValue(Double.parseDouble(String.valueOf(updates.get("value"))));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("minOrderAmount")) {
            try {
                voucher.setMinOrderAmount(Double.parseDouble(String.valueOf(updates.get("minOrderAmount"))));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("maxDiscountAmount")) {
            try {
                voucher.setMaxDiscountAmount(Double.parseDouble(String.valueOf(updates.get("maxDiscountAmount"))));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("usageLimit")) {
            try {
                String s = String.valueOf(updates.get("usageLimit"));
                voucher.setUsageLimit(s == null || s.isBlank() ? null : Integer.parseInt(s));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("isActive")) {
            voucher.setIsActive(Boolean.parseBoolean(String.valueOf(updates.get("isActive"))));
        }

        // Dates
        if (updates.containsKey("validFrom")) {
            try {
                voucher.setValidFrom(java.time.LocalDateTime.parse(String.valueOf(updates.get("validFrom"))));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("validTo")) {
            try {
                voucher.setValidTo(java.time.LocalDateTime.parse(String.valueOf(updates.get("validTo"))));
            } catch (Exception ignored) {}
        }

        Voucher saved = voucherRepository.save(voucher);
        // ensure collections fetched before mapping
        if (saved.getApplicableCategories() != null) saved.getApplicableCategories().size();
        if (saved.getApplicableBooks() != null) saved.getApplicableBooks().size();
        if (saved.getApplicableUsers() != null) saved.getApplicableUsers().size();
        return VoucherResponseDTO.fromEntity(saved);
    }

    /**
     * Tạo voucher mới (admin)
     */
    @Transactional
    public VoucherResponseDTO createVoucher(Map<String, Object> payload) {
        Voucher voucher = new Voucher();

        if (payload.containsKey("code")) voucher.setCode(String.valueOf(payload.get("code")).trim());
        if (payload.containsKey("name")) voucher.setName(String.valueOf(payload.get("name")).trim());
        if (payload.containsKey("description")) voucher.setDescription(payload.get("description") != null ? String.valueOf(payload.get("description")).trim() : null);

        if (payload.containsKey("type")) {
            try {
                voucher.setType(Voucher.VoucherType.valueOf(String.valueOf(payload.get("type")).toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (payload.containsKey("value")) {
            try { voucher.setValue(Double.parseDouble(String.valueOf(payload.get("value")))); } catch (Exception ignored) {}
        }

        if (payload.containsKey("minOrderAmount")) {
            try { voucher.setMinOrderAmount(Double.parseDouble(String.valueOf(payload.get("minOrderAmount")))); } catch (Exception ignored) {}
        }

        if (payload.containsKey("maxDiscountAmount")) {
            try { voucher.setMaxDiscountAmount(Double.parseDouble(String.valueOf(payload.get("maxDiscountAmount")))); } catch (Exception ignored) {}
        }

        if (payload.containsKey("usageLimit")) {
            try {
                String s = String.valueOf(payload.get("usageLimit"));
                voucher.setUsageLimit(s == null || s.isBlank() ? null : Integer.parseInt(s));
            } catch (Exception ignored) {}
        }

        if (payload.containsKey("validFrom")) {
            try { voucher.setValidFrom(java.time.LocalDateTime.parse(String.valueOf(payload.get("validFrom")))); } catch (Exception ignored) {}
        }
        if (payload.containsKey("validTo")) {
            try { voucher.setValidTo(java.time.LocalDateTime.parse(String.valueOf(payload.get("validTo")))); } catch (Exception ignored) {}
        }

        if (payload.containsKey("isActive")) {
            voucher.setIsActive(Boolean.parseBoolean(String.valueOf(payload.get("isActive"))));
        } else {
            voucher.setIsActive(true);
        }

        // createdBy - set if provided user id (from controller auth)
        if (payload.containsKey("createdById")) {
            try {
                Object obj = payload.get("createdById");
                Long createdById = obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong(String.valueOf(obj));
                userRepository.findById(createdById).ifPresent(voucher::setCreatedBy);
            } catch (Exception ignored) {}
        }
        voucher.setUsedCount(0);
        voucher.setIsDeleted(false);

        // Prevent duplicate code
        if (voucher.getCode() != null && voucherRepository.findByCodeAndIsDeletedFalse(voucher.getCode()).isPresent()) {
            throw new com.hutech.bookstore.exception.AppException("Mã voucher đã tồn tại", 409);
        }

        Voucher saved = voucherRepository.save(voucher);
        if (saved.getApplicableCategories() != null) saved.getApplicableCategories().size();
        if (saved.getApplicableBooks() != null) saved.getApplicableBooks().size();
        if (saved.getApplicableUsers() != null) saved.getApplicableUsers().size();
        return VoucherResponseDTO.fromEntity(saved);
    }
}


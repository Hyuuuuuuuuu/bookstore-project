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
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableVouchers(
            Double orderAmount,
            List<Long> categoryIds,
            List<Long> bookIds,
            Long userId) {
        
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> validVouchers = voucherRepository.findValidVouchers(now);
        
        // Fetch collections
        validVouchers.forEach(this::fetchLazyCollections);
        
        List<VoucherResponseDTO> availableVouchers = validVouchers.stream()
            .filter(voucher -> isVoucherApplicable(voucher, orderAmount, categoryIds, bookIds, userId))
            .map(VoucherResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", availableVouchers);
        data.put("total", availableVouchers.size());
        
        return data;
    }

    /**
     * Lấy tất cả voucher hợp lệ
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllValidVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> validVouchers = voucherRepository.findValidVouchers(now);
        validVouchers.forEach(this::fetchLazyCollections);
        
        List<VoucherResponseDTO> voucherDTOs = validVouchers.stream()
            .map(VoucherResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", voucherDTOs);
        data.put("total", voucherDTOs.size());
        
        return data;
    }

    /**
     * Lấy tất cả voucher cho admin
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllVouchersForAdmin(String status, String type, String search) {
        List<Voucher> vouchers = voucherRepository.findByIsDeletedFalse();

        // Filter logic
        if (status != null && !status.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            vouchers = vouchers.stream()
                .filter(voucher -> computeVoucherStatus(voucher, now).equals(status))
                .collect(Collectors.toList());
        }

        if (type != null && !type.isEmpty()) {
            vouchers = vouchers.stream()
                .filter(voucher -> voucher.getType().name().toLowerCase().equals(type.toLowerCase()))
                .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            vouchers = vouchers.stream()
                .filter(voucher ->
                    voucher.getCode().toLowerCase().contains(searchLower) ||
                    voucher.getName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }

        vouchers.forEach(this::fetchLazyCollections);

        List<VoucherResponseDTO> voucherDTOs = vouchers.stream()
            .map(VoucherResponseDTO::fromEntity)
            .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", voucherDTOs);
        data.put("total", voucherDTOs.size());

        return data;
    }

    /**
     * Lấy tất cả voucher (active và chưa bị xóa)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllVouchers() {
        List<Voucher> vouchers = voucherRepository.findByIsActiveTrueAndIsDeletedFalse();
        vouchers.forEach(this::fetchLazyCollections);

        List<VoucherResponseDTO> voucherDTOs = vouchers.stream()
            .map(VoucherResponseDTO::fromEntity)
            .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("vouchers", voucherDTOs);
        data.put("total", voucherDTOs.size());

        return data;
    }

    @Transactional(readOnly = true)
    public VoucherResponseDTO getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
            .filter(v -> !v.getIsDeleted())
            .orElseThrow(() -> new com.hutech.bookstore.exception.AppException("Voucher not found", 404));
        
        fetchLazyCollections(voucher);
        return VoucherResponseDTO.fromEntity(voucher);
    }

    @Transactional(readOnly = true)
    public VoucherResponseDTO getVoucherByCode(String code) {
        Voucher voucher = voucherRepository.findByCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new com.hutech.bookstore.exception.AppException("Voucher not found", 404));
        
        fetchLazyCollections(voucher);
        return VoucherResponseDTO.fromEntity(voucher);
    }

    @Transactional
    public VoucherResponseDTO updateVoucher(Long id, Map<String, Object> updates) {
        Voucher voucher = voucherRepository.findById(id)
            .filter(v -> !v.getIsDeleted())
            .orElseThrow(() -> new com.hutech.bookstore.exception.AppException("Voucher not found", 404));

        if (updates.containsKey("code")) voucher.setCode(String.valueOf(updates.get("code")).trim());
        if (updates.containsKey("name")) voucher.setName(String.valueOf(updates.get("name")).trim());
        if (updates.containsKey("description")) voucher.setDescription(updates.get("description") != null ? String.valueOf(updates.get("description")).trim() : null);
        
        if (updates.containsKey("type")) {
            try {
                voucher.setType(Voucher.VoucherType.valueOf(String.valueOf(updates.get("type")).toUpperCase()));
            } catch (Exception ignored) {}
        }
        
        if (updates.containsKey("value")) {
            try { voucher.setValue(Double.parseDouble(String.valueOf(updates.get("value")))); } catch (Exception ignored) {}
        }
        
        if (updates.containsKey("minOrderAmount")) {
            try { voucher.setMinOrderAmount(Double.parseDouble(String.valueOf(updates.get("minOrderAmount")))); } catch (Exception ignored) {}
        }
        
        if (updates.containsKey("maxDiscountAmount")) {
            try { voucher.setMaxDiscountAmount(Double.parseDouble(String.valueOf(updates.get("maxDiscountAmount")))); } catch (Exception ignored) {}
        }
        
        if (updates.containsKey("usageLimit")) {
            try {
                String s = String.valueOf(updates.get("usageLimit"));
                voucher.setUsageLimit(s == null || s.isBlank() ? null : Integer.parseInt(s));
            } catch (Exception ignored) {}
        }
        
        if (updates.containsKey("isActive")) {
            try { voucher.setIsActive(Boolean.parseBoolean(String.valueOf(updates.get("isActive")))); } catch (Exception ignored) {}
        }

        if (updates.containsKey("validFrom")) {
            try { voucher.setValidFrom(java.time.LocalDateTime.parse(String.valueOf(updates.get("validFrom")))); } catch (Exception ignored) {}
        }
        
        if (updates.containsKey("validTo")) {
            try { voucher.setValidTo(java.time.LocalDateTime.parse(String.valueOf(updates.get("validTo")))); } catch (Exception ignored) {}
        }

        Voucher saved = voucherRepository.save(voucher);
        fetchLazyCollections(saved);
        return VoucherResponseDTO.fromEntity(saved);
    }

    @Transactional
    public VoucherResponseDTO createVoucher(Map<String, Object> payload) {
        Voucher voucher = new Voucher();

        // Basic fields
        if (payload.containsKey("code")) voucher.setCode(String.valueOf(payload.get("code")).trim());
        if (payload.containsKey("name")) voucher.setName(String.valueOf(payload.get("name")).trim());
        if (payload.containsKey("description")) voucher.setDescription(payload.get("description") != null ? String.valueOf(payload.get("description")).trim() : null);

        // Enums & Numbers
        if (payload.containsKey("type")) {
            try { voucher.setType(Voucher.VoucherType.valueOf(String.valueOf(payload.get("type")).toUpperCase())); } catch (Exception ignored) {}
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

        // Dates
        if (payload.containsKey("validFrom")) {
            try { voucher.setValidFrom(java.time.LocalDateTime.parse(String.valueOf(payload.get("validFrom")))); } catch (Exception ignored) {}
        }
        if (payload.containsKey("validTo")) {
            try { voucher.setValidTo(java.time.LocalDateTime.parse(String.valueOf(payload.get("validTo")))); } catch (Exception ignored) {}
        }

        voucher.setIsActive(payload.containsKey("isActive") ? Boolean.parseBoolean(String.valueOf(payload.get("isActive"))) : true);
        voucher.setUsedCount(0);
        voucher.setIsDeleted(false);

        // Created By
        if (payload.containsKey("createdById")) {
            try {
                Object obj = payload.get("createdById");
                Long createdById = obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong(String.valueOf(obj));
                userRepository.findById(createdById).ifPresent(voucher::setCreatedBy);
            } catch (Exception ignored) {}
        }

        if (voucher.getCode() != null && voucherRepository.findByCodeAndIsDeletedFalse(voucher.getCode()).isPresent()) {
            throw new com.hutech.bookstore.exception.AppException("Mã voucher đã tồn tại", 409);
        }

        Voucher saved = voucherRepository.save(voucher);
        fetchLazyCollections(saved);
        return VoucherResponseDTO.fromEntity(saved);
    }

    /**
     * Xóa voucher (Soft delete) - MỚI THÊM
     */
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
            .filter(v -> !v.getIsDeleted())
            .orElseThrow(() -> new com.hutech.bookstore.exception.AppException("Voucher not found", 404));
        
        voucher.setIsDeleted(true);
        voucher.setIsActive(false); // Disable luôn khi xóa
        voucherRepository.save(voucher);
    }

    // Helper methods
    private void fetchLazyCollections(Voucher voucher) {
        if (voucher.getApplicableCategories() != null) voucher.getApplicableCategories().size();
        if (voucher.getApplicableBooks() != null) voucher.getApplicableBooks().size();
        if (voucher.getApplicableUsers() != null) voucher.getApplicableUsers().size();
    }

    private String computeVoucherStatus(Voucher voucher, LocalDateTime now) {
        if (!voucher.getIsActive()) return "inactive";
        if (voucher.getValidTo().isBefore(now)) return "expired";
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) return "expired";
        return "active";
    }

    private boolean isVoucherApplicable(Voucher voucher, Double orderAmount, List<Long> categoryIds, List<Long> bookIds, Long userId) {
        // Min Order Check
        if (orderAmount != null && voucher.getMinOrderAmount() != null && orderAmount < voucher.getMinOrderAmount()) return false;
        
        // Category Check
        if (voucher.getApplicableCategories() != null && !voucher.getApplicableCategories().isEmpty()) {
            if (categoryIds == null || categoryIds.isEmpty()) return false;
            boolean match = voucher.getApplicableCategories().stream().anyMatch(c -> categoryIds.contains(c.getId()));
            if (!match) return false;
        }
        
        // Book Check
        if (voucher.getApplicableBooks() != null && !voucher.getApplicableBooks().isEmpty()) {
            if (bookIds == null || bookIds.isEmpty()) return false;
            boolean match = voucher.getApplicableBooks().stream().anyMatch(b -> bookIds.contains(b.getId()));
            if (!match) return false;
        }
        
        // User Check
        if (voucher.getApplicableUsers() != null && !voucher.getApplicableUsers().isEmpty()) {
            if (userId == null) return false;
            boolean match = voucher.getApplicableUsers().stream().anyMatch(u -> u.getId().equals(userId));
            if (!match) return false;
        }
        
        // Usage Check
        if (userId != null && voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), userId)) return false;
        
        return true;
    }
}
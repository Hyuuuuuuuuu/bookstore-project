package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.VoucherDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.model.Voucher;
import com.hutech.bookstore.repository.UserRepository; // Cần thêm repo này
import com.hutech.bookstore.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository; // Dùng để lấy thông tin người tạo

    @Transactional(readOnly = true)
    public Map<String, Object> getAllVouchers(Integer page, Integer limit, String search, String status, String type) {
        Pageable pageable = PageRequest.of(
                page != null && page > 0 ? page - 1 : 0,
                limit != null && limit > 0 ? limit : 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // Xử lý Filter Status (Active)
        Boolean isActive = null;
        if (status != null && !status.equals("all")) {
            isActive = Boolean.parseBoolean(status);
        }

        // Xử lý Filter Type (Enum)
        Voucher.VoucherType typeEnum = null;
        if (type != null && !type.equals("all")) {
            try {
                typeEnum = Voucher.VoucherType.valueOf(type);
            } catch (IllegalArgumentException e) {
                // Ignore invalid type
            }
        }

        // Gọi Repository (Tìm kiếm + Lọc + Chỉ lấy voucher chưa bị xóa mềm)
        Page<Voucher> voucherPage = voucherRepository.findVouchersWithFilters(search, isActive, typeEnum, pageable);

        List<VoucherDTO> vouchers = voucherPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("vouchers", vouchers);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", voucherPage.getNumber() + 1);
        pagination.put("limit", voucherPage.getSize());
        pagination.put("total", voucherPage.getTotalElements());
        pagination.put("totalPages", voucherPage.getTotalPages());
        result.put("pagination", pagination);

        return result;
    }

    @Transactional(readOnly = true)
    public VoucherDTO getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException("Voucher không tồn tại hoặc đã bị xóa", 404));
        return convertToDTO(voucher);
    }

    @Transactional
    public VoucherDTO createVoucher(VoucherDTO dto) {
        if (voucherRepository.existsByCode(dto.getCode())) {
            throw new AppException("Mã voucher đã tồn tại", 400);
        }

        Voucher voucher = new Voucher();
        updateEntityFromDTO(voucher, dto);
        
        // --- QUAN TRỌNG: Xử lý người tạo (createdBy) ---
        // Vì Model yêu cầu createdBy không được null.
        // Tạm thời lấy User đầu tiên hoặc User có ID cố định làm người tạo.
        // Sau này bạn có thể thay bằng SecurityContextHolder để lấy user đang đăng nhập.
        User adminUser = userRepository.findAll().stream().findFirst()
                 .orElseThrow(() -> new AppException("Không tìm thấy Admin user để gán quyền tạo", 500));
        voucher.setCreatedBy(adminUser);
        
        voucher.setUsedCount(0);
        voucher.setIsDeleted(false);
        
        Voucher savedVoucher = voucherRepository.save(voucher);
        return convertToDTO(savedVoucher);
    }

    @Transactional
    public VoucherDTO updateVoucher(Long id, VoucherDTO dto) {
        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException("Voucher không tồn tại", 404));

        if (!voucher.getCode().equals(dto.getCode()) && voucherRepository.existsByCode(dto.getCode())) {
            throw new AppException("Mã voucher đã tồn tại", 400);
        }

        updateEntityFromDTO(voucher, dto);
        // Giữ nguyên createdBy và usedCount cũ
        Voucher updatedVoucher = voucherRepository.save(voucher);
        return convertToDTO(updatedVoucher);
    }

    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException("Voucher không tồn tại", 404));
        
        // Soft Delete: Chỉ đánh dấu là đã xóa
        voucher.setIsDeleted(true);
        voucherRepository.save(voucher);
    }

    // --- Helper Methods Mapping đúng theo Model của bạn ---
    private VoucherDTO convertToDTO(Voucher v) {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(v.getId());
        dto.setCode(v.getCode());
        dto.setName(v.getName()); // Mới thêm
        dto.setDescription(v.getDescription());
        
        if (v.getType() != null) {
            dto.setType(v.getType().name());
        }
        
        dto.setValue(v.getValue()); // Model dùng value
        dto.setMinOrderAmount(v.getMinOrderAmount());
        dto.setMaxDiscountAmount(v.getMaxDiscountAmount());
        dto.setUsageLimit(v.getUsageLimit());
        dto.setUsageCount(v.getUsedCount()); // Model dùng usedCount
        
        // Model dùng LocalDateTime
        dto.setStartDate(v.getValidFrom()); 
        dto.setEndDate(v.getValidTo());
        
        dto.setIsActive(v.getIsActive());
        return dto;
    }

    private void updateEntityFromDTO(Voucher v, VoucherDTO dto) {
        v.setCode(dto.getCode());
        v.setName(dto.getName());
        v.setDescription(dto.getDescription());
        
        if (dto.getType() != null) {
            v.setType(Voucher.VoucherType.valueOf(dto.getType().toUpperCase()));
        }
        
        v.setValue(dto.getValue());
        v.setMinOrderAmount(dto.getMinOrderAmount());
        v.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        v.setUsageLimit(dto.getUsageLimit());
        
        // Chuyển đổi thời gian từ DTO vào Entity
        v.setValidFrom(dto.getStartDate());
        v.setValidTo(dto.getEndDate());
        
        if (dto.getIsActive() != null) {
            v.setIsActive(dto.getIsActive());
        }
    }
}
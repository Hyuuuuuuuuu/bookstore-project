package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Voucher;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private String code;
    private String name;
    private String description;
    private String type; // PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING
    private Double value;
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
    private List<Long> applicableCategoryIds;
    private List<Long> applicableBookIds;
    private List<Long> applicableUserIds;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static VoucherResponseDTO fromEntity(Voucher voucher) {
        if (voucher == null) {
            return null;
        }
        
        VoucherResponseDTO dto = new VoucherResponseDTO();
        dto.setId(voucher.getId());
        dto.setCode(voucher.getCode());
        dto.setName(voucher.getName());
        dto.setDescription(voucher.getDescription());
        dto.setType(voucher.getType() != null ? voucher.getType().name() : null);
        dto.setValue(voucher.getValue());
        dto.setMinOrderAmount(voucher.getMinOrderAmount());
        dto.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        dto.setUsageLimit(voucher.getUsageLimit());
        dto.setUsedCount(voucher.getUsedCount());
        dto.setValidFrom(voucher.getValidFrom());
        dto.setValidTo(voucher.getValidTo());
        dto.setIsActive(voucher.getIsActive());
        dto.setIsDeleted(voucher.getIsDeleted());
        dto.setCreatedAt(voucher.getCreatedAt());
        dto.setUpdatedAt(voucher.getUpdatedAt());
        
        // Convert applicable categories
        if (voucher.getApplicableCategories() != null) {
            dto.setApplicableCategoryIds(voucher.getApplicableCategories().stream()
                .map(cat -> cat.getId())
                .collect(Collectors.toList()));
        }
        
        // Convert applicable books
        if (voucher.getApplicableBooks() != null) {
            dto.setApplicableBookIds(voucher.getApplicableBooks().stream()
                .map(book -> book.getId())
                .collect(Collectors.toList()));
        }
        
        // Convert applicable users
        if (voucher.getApplicableUsers() != null) {
            dto.setApplicableUserIds(voucher.getApplicableUsers().stream()
                .map(user -> user.getId())
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
}


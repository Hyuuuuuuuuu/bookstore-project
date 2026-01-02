package com.hutech.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDTO {
    private Long id;
    private String code;
    private String name;        // Mới thêm
    private String description;
    private String type;        // PERCENTAGE / FIXED_AMOUNT
    private Double value;       // Giá trị giảm
    
    private LocalDateTime startDate; // Mapping với validFrom
    private LocalDateTime endDate;   // Mapping với validTo
    
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    
    private Integer usageLimit;
    private Integer usageCount;
    
    private Boolean isActive;
}
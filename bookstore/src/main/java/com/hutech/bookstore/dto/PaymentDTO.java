package com.hutech.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private Double amount;
    private String method;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt;
    
    // Thông tin bổ sung từ Order để hiển thị
    private Long orderId;
    private String orderCode;
    private String description; // Chứa tên khách hàng hoặc mô tả
}
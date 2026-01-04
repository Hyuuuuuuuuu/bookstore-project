package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private Long orderId;
    
    private String transactionCode;
    
    private Double amount;
    
    private String method; // Enum as String
    
    private String status; // Enum as String
    
    private String paymentUrl;
    
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public static PaymentResponseDTO fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrder() != null ? payment.getOrder().getId() : null);
        dto.setTransactionCode(payment.getTransactionCode());
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod() != null ? payment.getMethod().name() : null);
        dto.setStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
        dto.setPaymentUrl(payment.getPaymentUrl());
        dto.setDescription(payment.getDescription());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        
        return dto;
    }
}


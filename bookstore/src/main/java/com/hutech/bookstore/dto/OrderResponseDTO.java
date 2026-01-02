package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private String orderCode;
    
    private Long userId;
    
    private String userName;
    
    private String userEmail;
    
    private String userPhone;
    
    private String userStatus;
    
    private List<OrderItemResponseDTO> orderItems;
    
    private Double totalPrice;
    
    private Double originalAmount;
    
    private Double discountAmount;
    
    private Long voucherId;
    
    private String paymentMethod; // Enum as String
    
    private String status; // Enum as String
    
    private Long shippingAddressId;
    
    private Long shippingProviderId;
    
    private Double shippingFee;
    
    private String paymentStatus; // Enum as String
    
    private String transactionId;
    
    private String qrCode;
    
    private LocalDateTime paidAt;
    
    private LocalDateTime confirmedAt;
    
    private LocalDateTime shippedAt;
    
    private LocalDateTime deliveredAt;
    
    private LocalDateTime cancelledAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public static OrderResponseDTO fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setUserId(order.getUser() != null ? order.getUser().getId() : null);
        dto.setUserName(order.getUser() != null ? order.getUser().getName() : null);
        dto.setUserEmail(order.getUser() != null ? order.getUser().getEmail() : null);
        dto.setUserPhone(order.getUser() != null ? order.getUser().getPhone() : null);
        dto.setUserStatus(order.getUser() != null && order.getUser().getStatus() != null ? order.getUser().getStatus().name().toLowerCase() : null);
        dto.setTotalPrice(order.getTotalPrice());
        dto.setOriginalAmount(order.getOriginalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setVoucherId(order.getVoucher() != null ? order.getVoucher().getId() : null);
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setShippingAddressId(order.getShippingAddress() != null ? order.getShippingAddress().getId() : null);
        dto.setShippingProviderId(order.getShippingProvider() != null ? order.getShippingProvider().getId() : null);
        dto.setShippingFee(order.getShippingFee());
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        dto.setTransactionId(order.getTransactionId());
        dto.setQrCode(order.getQrCode());
        dto.setPaidAt(order.getPaidAt());
        dto.setConfirmedAt(order.getConfirmedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        
        return dto;
    }
    
    public static OrderResponseDTO fromEntityWithItems(Order order, List<OrderItemResponseDTO> orderItems) {
        OrderResponseDTO dto = fromEntity(order);
        if (dto != null) {
            dto.setOrderItems(orderItems);
        }
        return dto;
    }
}


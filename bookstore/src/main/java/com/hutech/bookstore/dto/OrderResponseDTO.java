package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private String orderCode;
    private Long userId;
    
    // --- THÊM CÁC TRƯỜNG NÀY ---
    private String userName;
    private String userEmail;
    private String shippingName;
    private String shippingPhone;
    // ---------------------------

    private List<OrderItemResponseDTO> orderItems;
    private Double totalPrice;
    private Double originalAmount;
    private Double discountAmount;
    private Long voucherId;
    private String paymentMethod; 
    private String status; 
    private Long shippingAddressId;
    private Long shippingProviderId;
    private Double shippingFee;
    private String paymentStatus; 
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
        if (order == null) return null;
        
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        
        // Map User Info
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUserName(order.getUser().getName());   // Thêm
            dto.setUserEmail(order.getUser().getEmail()); // Thêm
        }
        
        // Map Shipping Info
        if (order.getShippingAddress() != null) {
            dto.setShippingAddressId(order.getShippingAddress().getId());
            dto.setShippingName(order.getShippingAddress().getName()); // Thêm
            dto.setShippingPhone(order.getShippingAddress().getPhone()); // Thêm
        }

        dto.setTotalPrice(order.getTotalPrice());
        dto.setOriginalAmount(order.getOriginalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setVoucherId(order.getVoucher() != null ? order.getVoucher().getId() : null);
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
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
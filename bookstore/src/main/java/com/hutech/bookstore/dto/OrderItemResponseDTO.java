package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private Long orderId;
    
    private BookResponseDTO book;
    
    private Integer quantity;
    
    private Double priceAtPurchase;
    
    private Double totalPrice; // quantity * priceAtPurchase
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public static OrderItemResponseDTO fromEntity(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setId(orderItem.getId());
        dto.setOrderId(orderItem.getOrder() != null ? orderItem.getOrder().getId() : null);
        dto.setBook(BookResponseDTO.fromEntity(orderItem.getBook()));
        dto.setQuantity(orderItem.getQuantity());
        dto.setPriceAtPurchase(orderItem.getPriceAtPurchase());
        dto.setTotalPrice(orderItem.getQuantity() * orderItem.getPriceAtPurchase());
        dto.setCreatedAt(orderItem.getCreatedAt());
        dto.setUpdatedAt(orderItem.getUpdatedAt());
        
        return dto;
    }
}


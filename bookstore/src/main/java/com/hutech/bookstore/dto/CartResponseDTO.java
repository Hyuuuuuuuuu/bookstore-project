package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Cart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private Long userId;
    
    private List<CartItemResponseDTO> items;
    
    private Integer totalItems;
    
    private Double totalPrice;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public static CartResponseDTO fromEntity(Cart cart) {
        if (cart == null) {
            return null;
        }
        
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser() != null ? cart.getUser().getId() : null);
        dto.setItems(cart.getItems() != null 
            ? cart.getItems().stream()
                .map(CartItemResponseDTO::fromEntity)
                .collect(Collectors.toList())
            : null);
        dto.setTotalItems(cart.getTotalItems());
        dto.setTotalPrice(cart.getTotalPrice());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());
        
        return dto;
    }
}


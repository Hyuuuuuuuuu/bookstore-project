package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private BookResponseDTO book;
    
    private Integer quantity;
    
    private Double totalPrice; // quantity * book.price
    
    private LocalDateTime addedAt;

    public static CartItemResponseDTO fromEntity(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }
        
        CartItemResponseDTO dto = new CartItemResponseDTO();
        dto.setId(cartItem.getId());
        dto.setBook(BookResponseDTO.fromEntity(cartItem.getBook()));
        dto.setQuantity(cartItem.getQuantity());
        dto.setTotalPrice(cartItem.getBook().getPrice() * cartItem.getQuantity());
        dto.setAddedAt(cartItem.getAddedAt());
        
        return dto;
    }
}


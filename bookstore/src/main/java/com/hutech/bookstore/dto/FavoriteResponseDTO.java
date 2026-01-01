package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Favorite;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("bookId")
    private Long bookId;
    
    private BookResponseDTO book;
    
    private Boolean isFavourite;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static FavoriteResponseDTO fromEntity(Favorite favorite) {
        if (favorite == null) {
            return null;
        }
        
        FavoriteResponseDTO dto = new FavoriteResponseDTO();
        dto.setId(favorite.getId());
        dto.setUserId(favorite.getUser() != null ? favorite.getUser().getId() : null);
        dto.setBookId(favorite.getBook() != null ? favorite.getBook().getId() : null);
        dto.setBook(favorite.getBook() != null ? BookResponseDTO.fromEntity(favorite.getBook()) : null);
        dto.setIsFavourite(favorite.getIsFavourite());
        dto.setIsDeleted(favorite.getIsDeleted());
        dto.setCreatedAt(favorite.getCreatedAt());
        dto.setUpdatedAt(favorite.getUpdatedAt());
        
        return dto;
    }
}


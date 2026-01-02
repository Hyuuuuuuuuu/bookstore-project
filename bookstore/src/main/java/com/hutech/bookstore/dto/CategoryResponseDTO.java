package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private String name;
    private String description;
    private String status;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static CategoryResponseDTO fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setStatus(category.getStatus());
        dto.setDescription(category.getDescription());
        dto.setIsDeleted(category.getIsDeleted());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        
        return dto;
    }
}


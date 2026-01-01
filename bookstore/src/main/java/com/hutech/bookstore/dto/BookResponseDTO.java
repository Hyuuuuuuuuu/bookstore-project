package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private String title;
    private String author;
    private Double price;
    private Integer stock;
    private String description;
    private String imageUrl;
    
    // --- BỔ SUNG TRƯỜNG NÀY ĐỂ FIX LỖI SERVICE ---
    private Long categoryId; 
    // ---------------------------------------------

    @JsonProperty("category")
    private CategoryResponseDTO category;
    
    private String isbn;
    private String publisher;
    private LocalDate publicationDate;
    private Integer pages;
    private String format;
    private String dimensions;
    private Double weight;
    private String fileUrl;
    private DigitalFileDTO digitalFile;
    private Integer viewCount;
    private Boolean isActive;
    private String status;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigitalFileDTO {
        private String filePath;
        private Long fileSize;
        private String mimeType;
        private Integer duration;
    }
    
    public static BookResponseDTO fromEntity(Book book) {
        if (book == null) {
            return null;
        }
        
        BookResponseDTO dto = new BookResponseDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPrice(book.getPrice());
        dto.setStock(book.getStock());
        dto.setDescription(book.getDescription());
        dto.setImageUrl(book.getImageUrl());
        
        // Cập nhật logic mapping cho Category
        if (book.getCategory() != null) {
            dto.setCategory(CategoryResponseDTO.fromEntity(book.getCategory()));
            dto.setCategoryId(book.getCategory().getId()); // Map thêm ID để tiện xử lý
        }
        
        dto.setIsbn(book.getIsbn());
        dto.setPublisher(book.getPublisher());
        dto.setPublicationDate(book.getPublicationDate());
        dto.setPages(book.getPages());
        dto.setFormat(book.getFormat() != null ? book.getFormat().name() : null);
        dto.setDimensions(book.getDimensions());
        dto.setWeight(book.getWeight());
        dto.setFileUrl(book.getFileUrl());
        
        if (book.getDigitalFile() != null) {
            DigitalFileDTO digitalFileDTO = new DigitalFileDTO();
            digitalFileDTO.setFilePath(book.getDigitalFile().getFilePath());
            digitalFileDTO.setFileSize(book.getDigitalFile().getFileSize());
            digitalFileDTO.setMimeType(book.getDigitalFile().getMimeType());
            digitalFileDTO.setDuration(book.getDigitalFile().getDuration());
            dto.setDigitalFile(digitalFileDTO);
        }
        
        dto.setViewCount(book.getViewCount());
        dto.setIsActive(book.getIsActive());
        dto.setStatus(book.getStatus() != null ? book.getStatus().name() : null);
        dto.setIsDeleted(book.getIsDeleted());
        dto.setCreatedAt(book.getCreatedAt());
        dto.setUpdatedAt(book.getUpdatedAt());
        
        return dto;
    }
}
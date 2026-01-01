package com.hutech.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(unique = true)
    private String isbn;

    @Column(length = 200)
    private String publisher;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    private Integer pages = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookFormat format = BookFormat.PAPERBACK;

    @Column(length = 50)
    private String dimensions;

    private Double weight = 0.0;

    @Column(name = "file_url")
    private String fileUrl;

    @Embedded
    private DigitalFile digitalFile;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status = BookStatus.AVAILABLE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BookFormat {
        HARDCOVER, PAPERBACK, EBOOK, AUDIOBOOK
    }

    public enum BookStatus {
        AVAILABLE, OUT_OF_STOCK, DISCONTINUED, COMING_SOON
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigitalFile {
        @Column(name = "digital_file_path")
        private String filePath;

        @Column(name = "digital_file_size")
        private Long fileSize;

        @Column(name = "digital_mime_type")
        private String mimeType;

        @Column(name = "digital_duration")
        private Integer duration; // in seconds for audiobooks
    }
}


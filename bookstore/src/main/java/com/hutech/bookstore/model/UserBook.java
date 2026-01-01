package com.hutech.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_books", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "book_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_type", nullable = false)
    private BookType bookType;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "last_download_at")
    private LocalDateTime lastDownloadAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_book_id")
    private List<DownloadHistory> downloadHistory = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BookType {
        PHYSICAL, EBOOK, AUDIOBOOK
    }

    @Entity
    @Table(name = "download_history")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadHistory {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Enumerated(EnumType.STRING)
        @Column(name = "download_type", nullable = false)
        private DownloadType downloadType;

        @Column(name = "ip_address", nullable = false, length = 50)
        private String ipAddress;

        @Column(name = "user_agent", length = 500)
        private String userAgent;

        @Column(name = "file_size")
        private Long fileSize;

        @Column(name = "download_duration")
        private Integer downloadDuration; // in seconds

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private DownloadStatus status = DownloadStatus.STARTED;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        public enum DownloadType {
            DOWNLOAD, STREAM
        }

        public enum DownloadStatus {
            STARTED, COMPLETED, FAILED
        }
    }
}


package com.hutech.bookstore.service;

import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.model.UserBook;
import com.hutech.bookstore.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final UserBookRepository userBookRepository;
    private static final int MAX_DOWNLOAD_COUNT = 3;

    /**
     * Tạo download link tạm thời cho sách
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateDownloadLink(User user, Long bookId) {
        UserBook userBook = userBookRepository.findByUserAndBookIdAndIsActiveTrue(user, bookId)
                .orElseThrow(() -> new AppException("Book not found in your library", 404));

        // Kiểm tra limit download
        if (userBook.getDownloadCount() >= MAX_DOWNLOAD_COUNT) {
            throw new AppException("Bạn đã đạt giới hạn tải xuống (3 lần). Vui lòng liên hệ hỗ trợ nếu cần thêm lượt tải.", 400);
        }

        // Tạo token tạm thời (có thể lưu vào cache/redis trong production)
        String token = UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("downloadUrl", "/api/download/file/" + bookId + "?token=" + token);
        response.put("streamUrl", "/api/download/stream/" + bookId + "?token=" + token);
        response.put("token", token);
        response.put("remainingDownloads", MAX_DOWNLOAD_COUNT - userBook.getDownloadCount());
        
        return response;
    }

    /**
     * Tăng download count khi user download file
     */
    @Transactional
    public void incrementDownloadCount(User user, Long bookId, String ipAddress, String userAgent) {
        UserBook userBook = userBookRepository.findByUserAndBookIdAndIsActiveTrue(user, bookId)
                .orElseThrow(() -> new AppException("Book not found in your library", 404));

        // Kiểm tra limit
        if (userBook.getDownloadCount() >= MAX_DOWNLOAD_COUNT) {
            throw new AppException("Bạn đã đạt giới hạn tải xuống (3 lần)", 400);
        }

        // Tăng download count
        userBook.setDownloadCount(userBook.getDownloadCount() + 1);
        userBook.setLastDownloadAt(LocalDateTime.now());
        
        // Tạo download history record
        UserBook.DownloadHistory history = new UserBook.DownloadHistory();
        history.setDownloadType(UserBook.DownloadHistory.DownloadType.DOWNLOAD);
        history.setIpAddress(ipAddress != null ? ipAddress : "unknown");
        history.setUserAgent(userAgent);
        history.setFileSize(userBook.getFileSize());
        history.setStatus(UserBook.DownloadHistory.DownloadStatus.COMPLETED);
        
        if (userBook.getDownloadHistory() == null) {
            userBook.setDownloadHistory(new java.util.ArrayList<>());
        }
        userBook.getDownloadHistory().add(history);
        
        userBookRepository.save(userBook);
    }

    /**
     * Lấy thông tin download của sách
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDownloadInfo(User user, Long bookId) {
        UserBook userBook = userBookRepository.findByUserAndBookIdAndIsActiveTrue(user, bookId)
                .orElseThrow(() -> new AppException("Book not found in your library", 404));

        Map<String, Object> info = new HashMap<>();
        info.put("downloadCount", userBook.getDownloadCount());
        info.put("maxDownloads", MAX_DOWNLOAD_COUNT);
        info.put("remainingDownloads", MAX_DOWNLOAD_COUNT - userBook.getDownloadCount());
        info.put("lastDownloadAt", userBook.getLastDownloadAt());
        info.put("canDownload", userBook.getDownloadCount() < MAX_DOWNLOAD_COUNT);
        
        return info;
    }

    /**
     * Lấy thông tin offline của sách
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOfflineInfo(User user, Long bookId) {
        UserBook userBook = userBookRepository.findByUserAndBookIdAndIsActiveTrue(user, bookId)
                .orElseThrow(() -> new AppException("Book not found in your library", 404));

        Map<String, Object> offlineInfo = new HashMap<>();
        
        // Book info
        Map<String, Object> bookInfo = new HashMap<>();
        if (userBook.getBook() != null) {
            bookInfo.put("_id", userBook.getBook().getId());
            bookInfo.put("id", userBook.getBook().getId());
            bookInfo.put("title", userBook.getBook().getTitle());
            bookInfo.put("author", userBook.getBook().getAuthor());
            bookInfo.put("description", userBook.getBook().getDescription());
            bookInfo.put("imageUrl", userBook.getBook().getImageUrl());
        }
        offlineInfo.put("book", bookInfo);
        
        // Offline access info
        Map<String, Object> offlineAccess = new HashMap<>();
        offlineAccess.put("filePath", userBook.getFilePath());
        offlineAccess.put("fileSize", userBook.getFileSize());
        offlineAccess.put("mimeType", userBook.getMimeType());
        offlineAccess.put("bookType", userBook.getBookType() != null ? userBook.getBookType().name().toLowerCase() : null);
        offlineInfo.put("offlineAccess", offlineAccess);
        
        // Download stats
        Map<String, Object> downloadStats = new HashMap<>();
        downloadStats.put("totalDownloads", userBook.getDownloadCount());
        downloadStats.put("maxDownloads", MAX_DOWNLOAD_COUNT);
        downloadStats.put("remainingDownloads", MAX_DOWNLOAD_COUNT - userBook.getDownloadCount());
        downloadStats.put("lastDownloadAt", userBook.getLastDownloadAt());
        offlineInfo.put("downloadStats", downloadStats);
        
        return offlineInfo;
    }
}


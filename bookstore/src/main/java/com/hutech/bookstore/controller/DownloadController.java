package com.hutech.bookstore.controller;

import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.model.UserBook;
import com.hutech.bookstore.repository.UserBookRepository;
import com.hutech.bookstore.service.DownloadService;
import com.hutech.bookstore.service.FileUploadService;
import com.hutech.bookstore.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;
    private final FileUploadService fileUploadService;
    private final UserBookRepository userBookRepository;

    /**
     * Tạo download link tạm thời
     * GET /api/download/temp/{bookId}
     */
    @GetMapping("/temp/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateDownloadLink(@PathVariable Long bookId) {
        User user = getCurrentUser();
        Map<String, Object> data = downloadService.generateDownloadLink(user, bookId);
        return ResponseEntity.ok(ApiResponse.success(data, "Download link generated successfully"));
    }

    /**
     * Download file
     * GET /api/download/file/{bookId}?token=...
     */
    @GetMapping("/file/{bookId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long bookId,
            @RequestParam(required = false) String token,
            HttpServletRequest request) {
        User user = getCurrentUser();
        
        // Lấy UserBook để kiểm tra quyền và lấy file path
        UserBook userBook = userBookRepository.findByUserAndBookIdAndIsActiveTrue(user, bookId)
                .orElseThrow(() -> new AppException("Book not found in your library", 404));
        
        // Kiểm tra còn lượt download không
        if (userBook.getDownloadCount() >= 3) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .body(null);
        }
        
        try {
            // Tăng download count
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            downloadService.incrementDownloadCount(user, bookId, ipAddress, userAgent);
            
            // Lấy file resource
            if (userBook.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = fileUploadService.loadFileAsResource(userBook.getFilePath());
            
            if (resource == null || !resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = userBook.getMimeType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stream file (không tính vào download limit)
     * GET /api/download/stream/{bookId}?token=...
     */
    @GetMapping("/stream/{bookId}")
    public ResponseEntity<Resource> streamFile(
            @PathVariable Long bookId,
            @RequestParam(required = false) String token,
            HttpServletRequest request) {
        User user = getCurrentUser();
        
        // Lấy UserBook để kiểm tra quyền
        UserBook userBook = userBookRepository.findByUserAndBookIdAndIsActiveTrue(user, bookId)
                .orElseThrow(() -> new AppException("Book not found in your library", 404));
        
        try {
            // Lấy file resource
            if (userBook.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = fileUploadService.loadFileAsResource(userBook.getFilePath());
            
            if (resource == null || !resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = userBook.getMimeType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy thông tin download
     * GET /api/download/info/{bookId}
     */
    @GetMapping("/info/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadInfo(@PathVariable Long bookId) {
        User user = getCurrentUser();
        Map<String, Object> data = downloadService.getDownloadInfo(user, bookId);
        return ResponseEntity.ok(ApiResponse.success(data, "Download info retrieved successfully"));
    }

    /**
     * Lấy thông tin offline
     * GET /api/download/offline-info/{bookId}
     */
    @GetMapping("/offline-info/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOfflineInfo(@PathVariable Long bookId) {
        User user = getCurrentUser();
        Map<String, Object> data = downloadService.getOfflineInfo(user, bookId);
        return ResponseEntity.ok(ApiResponse.success(data, "Offline info retrieved successfully"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null ||
                authentication.getPrincipal() == null ||
                !(authentication.getPrincipal() instanceof User)) {
            throw new RuntimeException("Authentication required");
        }
        return (User) authentication.getPrincipal();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}


package com.hutech.bookstore.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.max-file-size:5242880}") // 5MB default
    private long maxFileSize;
    
    @Value("${app.upload.dir:}")
    private String configuredUploadDir;

    private String uploadDir;

    @PostConstruct
    private void init() {
        try {
            // If a configured upload directory is provided via property, use it.
            if (configuredUploadDir != null && !configuredUploadDir.isBlank()) {
                File uploadsDir = new File(configuredUploadDir);
                if (!uploadsDir.exists()) {
                    boolean created = uploadsDir.mkdirs();
                    if (!created) {
                        log.warn("Could not create configured uploadDir: {}", uploadsDir.getAbsolutePath());
                    }
                }

                // Ensure subdirectories exist
                new File(uploadsDir, "avatars").mkdirs();
                new File(uploadsDir, "books").mkdirs();
                new File(uploadsDir, "documents").mkdirs();

                this.uploadDir = uploadsDir.getAbsolutePath();
                log.info("File upload directory initialized from config: {}", this.uploadDir);
                return;
            }

            // Default behavior: resolve classpath static/uploads (works in IDE)
            File staticDir = ResourceUtils.getFile("classpath:static");
            File uploadsDir = new File(staticDir, "uploads");

            // Ensure uploads directory exists
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            // Ensure subdirectories exist
            new File(uploadsDir, "avatars").mkdirs();
            new File(uploadsDir, "books").mkdirs();
            new File(uploadsDir, "documents").mkdirs();

            this.uploadDir = uploadsDir.getAbsolutePath();
            log.info("File upload directory initialized: {}", this.uploadDir);

        } catch (Exception e) {
            log.error("Failed to initialize upload directory", e);
            // Fallback to configuredUploadDir if present, otherwise to relative path
            this.uploadDir = (configuredUploadDir != null && !configuredUploadDir.isBlank())
                ? configuredUploadDir
                : "src/main/resources/static/uploads";
            log.warn("Using fallback upload directory: {}", this.uploadDir);
        }
    }

    // Allowed file types for images
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    // Allowed file types for documents
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
        "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public String uploadBookImage(MultipartFile file) throws IOException {
        return uploadFile(file, "books", ALLOWED_IMAGE_TYPES, "book-image");
    }

    public String uploadAvatar(MultipartFile file) throws IOException {
        return uploadFile(file, "avatars", ALLOWED_IMAGE_TYPES, "avatar-image");
    }

    public String uploadDocument(MultipartFile file) throws IOException {
        return uploadFile(file, "documents", ALLOWED_DOCUMENT_TYPES, "document");
    }

    private String uploadFile(MultipartFile file, String subDir, List<String> allowedTypes, String filePrefix) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Check file type
        if (!allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + allowedTypes);
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = filePrefix + "_" + UUID.randomUUID().toString() + "." + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File uploaded successfully: {}", uniqueFilename);

        // Return relative path for database storage
        return subDir + "/" + uniqueFilename;
    }

    public void deleteFile(String filePath) {
        try {
            if (filePath != null && !filePath.isEmpty()) {
                Path fullPath = Paths.get(uploadDir, filePath);
                Files.deleteIfExists(fullPath);
                log.info("File deleted successfully: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
        }
    }

    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        Path fullPath = Paths.get(uploadDir, filePath);
        return Files.exists(fullPath);
    }

    public String getFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        // Return the URL path for static resources
        // Files are served from /uploads/ path in Spring Boot static resources
        return "/uploads/" + filePath;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}

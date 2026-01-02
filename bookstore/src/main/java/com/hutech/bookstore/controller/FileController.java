package com.hutech.bookstore.controller;

import com.hutech.bookstore.service.FileUploadService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/upload/book-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadBookImage(
            @RequestParam("file") MultipartFile file) {

        try {
            String filePath = fileUploadService.uploadBookImage(file);
            String fileUrl = fileUploadService.getFileUrl(filePath);

            Map<String, Object> data = new HashMap<>();
            data.put("filePath", filePath);
            data.put("fileUrl", fileUrl);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", file.getSize());

            return ResponseEntity.ok(ApiResponse.success(data, "Book image uploaded successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to upload book image", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "Failed to upload file"));
        }
    }

    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {

        try {
            String filePath = fileUploadService.uploadAvatar(file);
            String fileUrl = fileUploadService.getFileUrl(filePath);

            Map<String, Object> data = new HashMap<>();
            data.put("filePath", filePath);
            data.put("fileUrl", fileUrl);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", file.getSize());

            return ResponseEntity.ok(ApiResponse.success(data, "Avatar uploaded successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid avatar upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to upload avatar", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "Failed to upload file"));
        }
    }

    @PostMapping(value = "/upload/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        try {
            String filePath = fileUploadService.uploadDocument(file);
            String fileUrl = fileUploadService.getFileUrl(filePath);

            Map<String, Object> data = new HashMap<>();
            data.put("filePath", filePath);
            data.put("fileUrl", fileUrl);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", file.getSize());

            return ResponseEntity.ok(ApiResponse.success(data, "Document uploaded successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid document upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to upload document", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "Failed to upload file"));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteFile(@RequestParam("filePath") String filePath) {
        try {
            fileUploadService.deleteFile(filePath);
            return ResponseEntity.ok(ApiResponse.success("File deleted successfully", "File deleted"));

        } catch (Exception e) {
            log.error("Failed to delete file: {}", filePath, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "Failed to delete file"));
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFileExists(@RequestParam("filePath") String filePath) {
        boolean exists = fileUploadService.fileExists(filePath);
        Map<String, Boolean> data = Map.of("exists", exists);

        return ResponseEntity.ok(ApiResponse.success(data, "File existence checked"));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("uploadDir", fileUploadService.getUploadDir());
        data.put("maxFileSize", fileUploadService.getMaxFileSize());
        data.put("maxFileSizeMB", fileUploadService.getMaxFileSize() / 1024 / 1024);

        return ResponseEntity.ok(ApiResponse.success(data, "File upload configuration"));
    }
}
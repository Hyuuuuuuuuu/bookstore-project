package com.hutech.bookstore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*") // Cho phép Frontend gọi
public class UploadController {

    // Thư mục lưu ảnh: project_folder/uploads
    private final String UPLOAD_DIR = "uploads/";

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Tạo tên file unique để tránh trùng
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            
            // Tạo thư mục nếu chưa có
            if (!Files.exists(Paths.get(UPLOAD_DIR))) {
                Files.createDirectories(Paths.get(UPLOAD_DIR));
            }

            // Lưu file
            Files.write(path, file.getBytes());

            // Trả về đường dẫn file (Frontend sẽ lưu đường dẫn này vào field imageUrl của Book)
            // Lưu ý: Cần cấu hình Static Resource Handler để xem được ảnh này
            return ResponseEntity.ok().body("/uploads/" + fileName);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }
}
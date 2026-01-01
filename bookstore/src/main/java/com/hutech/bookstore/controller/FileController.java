package com.hutech.bookstore.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
public class FileController {

    private String UPLOAD_DIR;

    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        try {
            // Initialize UPLOAD_DIR if not set
            if (UPLOAD_DIR == null) {
                String resourcePath = getClass().getClassLoader().getResource("").getPath();
                UPLOAD_DIR = resourcePath + "static/uploads/";
            }

            Path filePath = Paths.get(UPLOAD_DIR + "avatars/" + filename);
            System.out.println("Looking for avatar at: " + filePath.toString());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                System.out.println("Avatar found: " + filename);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                System.out.println("Avatar not found: " + filename);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error serving avatar: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/books/{filename:.+}")
    public ResponseEntity<Resource> getBookImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR + "books/" + filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

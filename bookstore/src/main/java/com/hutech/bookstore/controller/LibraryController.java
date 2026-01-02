package com.hutech.bookstore.controller;

import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.model.UserBook;
import com.hutech.bookstore.repository.UserBookRepository;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final UserBookRepository userBookRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyLibrary() {
        User user = getCurrentUser();

        List<UserBook> userBooks = userBookRepository.findByUserAndIsActiveTrue(user);

        List<Map<String, Object>> books = userBooks.stream().map(ub -> {
            Book b = ub.getBook();
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            // Mirror frontend shape: userBook._id and nested bookId object
            m.put("_id", ub.getId());
            java.util.Map<String, Object> bookObj = new java.util.HashMap<>();
            if (b != null) {
                bookObj.put("_id", b.getId());
                bookObj.put("title", b.getTitle());
                bookObj.put("author", b.getAuthor());
                bookObj.put("imageUrl", b.getImageUrl());
                bookObj.put("description", b.getDescription());
            }
            m.put("bookId", bookObj);
            m.put("bookType", ub.getBookType() != null ? ub.getBookType().name().toLowerCase() : null);
            m.put("filePath", ub.getFilePath());
            m.put("fileSize", ub.getFileSize());
            m.put("downloadCount", ub.getDownloadCount());
            m.put("expiresAt", ub.getExpiresAt());
            return m;
        }).collect(Collectors.toList());

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("books", books);
        data.put("total", books.size());

        return ResponseEntity.ok(ApiResponse.success(data, "Library retrieved successfully"));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLibraryBook(@PathVariable Long bookId) {
        User user = getCurrentUser();

        UserBook ub = userBookRepository.findByUserAndBookIdAndIsActiveTrue(user, bookId)
                .orElseThrow(() -> new RuntimeException("Book not found in user library"));

        Book b = ub.getBook();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("_id", ub.getId());
        java.util.Map<String, Object> bookObj = new java.util.HashMap<>();
        if (b != null) {
            bookObj.put("_id", b.getId());
            bookObj.put("title", b.getTitle());
            bookObj.put("author", b.getAuthor());
            bookObj.put("description", b.getDescription());
            bookObj.put("imageUrl", b.getImageUrl());
        }
        data.put("bookId", bookObj);
        data.put("bookType", ub.getBookType() != null ? ub.getBookType().name().toLowerCase() : null);
        data.put("filePath", ub.getFilePath());
        data.put("fileSize", ub.getFileSize());
        data.put("downloadCount", ub.getDownloadCount());
        data.put("expiresAt", ub.getExpiresAt());

        return ResponseEntity.ok(ApiResponse.success(data, "Library book retrieved successfully"));
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
}



package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.BookResponseDTO;
import com.hutech.bookstore.service.BookService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBooks(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String stock,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        
        Map<String, Object> data = bookService.getBooks(
            page, limit, search, categoryId, minPrice, maxPrice, stock, sortBy, sortOrder
        );
        
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Books retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponseDTO>> getBookById(@PathVariable Long id) {
        BookResponseDTO bookDTO = bookService.getBookById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, bookDTO, "Book retrieved successfully"));
    }

    /**
     * Cập nhật sách (Admin)
     * PUT /api/books/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponseDTO>> updateBook(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        BookResponseDTO updated = bookService.updateBook(id, payload);
        return ResponseEntity.ok(ApiResponse.success(updated, "Book updated successfully"));
    }

    /**
     * Tạo sách mới (Admin)
     * POST /api/books
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookResponseDTO>> createBook(@RequestBody Map<String, Object> payload) {
        BookResponseDTO created = bookService.createBook(payload);
        return ResponseEntity.status(201).body(ApiResponse.created(created, "Book created successfully"));
    }
}


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
    @PostMapping
    public ResponseEntity<ApiResponse<BookResponseDTO>> createBook(@RequestBody BookResponseDTO bookDTO) {
        // GỌI SERVICE THẬT
        BookResponseDTO newBook = bookService.createBook(bookDTO); 
        return ResponseEntity.ok(new ApiResponse<>(201, newBook, "Sách đã được tạo thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponseDTO>> updateBook(@PathVariable Long id, @RequestBody BookResponseDTO bookDTO) {
        // GỌI SERVICE THẬT
        BookResponseDTO updatedBook = bookService.updateBook(id, bookDTO);
        return ResponseEntity.ok(new ApiResponse<>(200, updatedBook, "Sách đã cập nhật thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        // GỌI SERVICE THẬT
        bookService.deleteBook(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "Sách đã được xóa thành công"));
    }
}


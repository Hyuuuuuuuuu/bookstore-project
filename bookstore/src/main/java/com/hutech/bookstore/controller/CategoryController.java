package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.CategoryResponseDTO;
import com.hutech.bookstore.service.CategoryService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder) {
        
        Map<String, Object> data = categoryService.getCategories(search, sortBy, sortOrder);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Categories retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable Long id) {
        CategoryResponseDTO categoryDTO = categoryService.getCategoryById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, categoryDTO, "Category retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(@RequestBody CategoryResponseDTO dto) {
        CategoryResponseDTO newCategory = categoryService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, newCategory, "Category created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable Long id, 
            @RequestBody CategoryResponseDTO dto) {
        CategoryResponseDTO updatedCategory = categoryService.updateCategory(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(200, updatedCategory, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "Category deleted successfully"));
    }
}
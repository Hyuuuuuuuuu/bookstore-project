package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.CategoryResponseDTO;
import com.hutech.bookstore.service.CategoryService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategories(
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder) {
        
        Map<String, Object> data = categoryService.getCategories(sortBy, sortOrder);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Categories retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable Long id) {
        CategoryResponseDTO categoryDTO = categoryService.getCategoryById(id);
        return ResponseEntity.ok(new ApiResponse<>(200, categoryDTO, "Category retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String description = payload.get("description");
        String status = payload.get("status");
        CategoryResponseDTO created = categoryService.createCategory(name, description, status);
        return ResponseEntity.status(201).body(ApiResponse.created(created, "Category created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String description = payload.get("description");
        String status = payload.get("status");
        CategoryResponseDTO updated = categoryService.updateCategory(id, name, description, status);
        return ResponseEntity.ok(ApiResponse.success(updated, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", "Category deleted"));
    }
}


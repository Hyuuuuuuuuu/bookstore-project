package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.CategoryResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.Category;
import com.hutech.bookstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy danh sách categories với sorting
     */
    public Map<String, Object> getCategories(String sortBy, String sortOrder) {
        // Tạo Sort object
        Sort sort = sortOrder != null && sortOrder.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy != null ? sortBy : "name").descending() 
            : Sort.by(sortBy != null ? sortBy : "name").ascending();
        
        // Lấy tất cả categories
        List<Category> categories = categoryRepository.findAll(sort);
        
        // Lọc các category chưa bị xóa
        List<Category> activeCategories = categories.stream()
            .filter(cat -> !cat.getIsDeleted())
            .collect(Collectors.toList());
        
        // Convert to DTOs
        List<CategoryResponseDTO> categoryDTOs = activeCategories.stream()
            .map(CategoryResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryDTOs);
        data.put("total", categoryDTOs.size());
        
        return data;
    }

    /**
     * Lấy category theo ID
     */
    public CategoryResponseDTO getCategoryById(Long id) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isEmpty() || categoryOpt.get().getIsDeleted()) {
            throw new AppException("Category not found", 404);
        }
        return CategoryResponseDTO.fromEntity(categoryOpt.get());
    }

    /**
     * Tạo mới category
     */
    @Transactional
    public CategoryResponseDTO createCategory(String name, String description, String status) {
        if (name == null || name.trim().isEmpty()) {
            throw new AppException("Category name is required", 400);
        }
        categoryRepository.findByNameAndIsDeletedFalse(name.trim()).ifPresent(c -> {
            throw new AppException("Category already exists", 409);
        });

        Category category = new Category();
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : null);
        category.setStatus(status != null && !status.isBlank() ? status.trim() : "active");
        category.setIsDeleted(false);
        Category saved = categoryRepository.save(category);
        return CategoryResponseDTO.fromEntity(saved);
    }

    /**
     * Cập nhật category
     */
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, String name, String description, String status) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Category not found", 404));
        if (category.getIsDeleted()) throw new AppException("Category not found", 404);

        if (name != null && !name.trim().isEmpty() && !name.trim().equalsIgnoreCase(category.getName())) {
            // check duplicate name
            categoryRepository.findByNameAndIsDeletedFalse(name.trim()).ifPresent(other -> {
                if (!other.getId().equals(id)) throw new AppException("Category name already in use", 409);
            });
            category.setName(name.trim());
        }
        if (description != null) category.setDescription(description.trim());
        if (status != null && !status.isBlank()) category.setStatus(status.trim());
        Category updated = categoryRepository.save(category);
        return CategoryResponseDTO.fromEntity(updated);
    }

    /**
     * Xóa category (soft delete)
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Category not found", 404));
        if (category.getIsDeleted()) return;
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }
}


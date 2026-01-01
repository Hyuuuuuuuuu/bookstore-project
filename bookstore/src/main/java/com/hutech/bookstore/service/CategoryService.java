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
@Transactional(readOnly = true) // Mặc định là chỉ đọc để tối ưu hiệu năng
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy danh sách categories với sorting
     */
    public Map<String, Object> getCategories(String search, String sortBy, String sortOrder) {
        // Tạo Sort object
        Sort sort = sortOrder != null && sortOrder.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy != null ? sortBy : "name").descending() 
            : Sort.by(sortBy != null ? sortBy : "name").ascending();
        
        // Lấy tất cả categories
        List<Category> categories = categoryRepository.findAll(sort);

        if (search != null && !search.trim().isEmpty()) {
        // Nếu có từ khóa -> Gọi hàm tìm kiếm trong Repository
        categories = categoryRepository.searchCategories(search.trim(), sort);
        } else {
        // Nếu KHÔNG có từ khóa -> Lấy tất cả như cũ
        categories = categoryRepository.findAll(sort);
        }
        
        // Lọc các category chưa bị xóa (Soft Delete check)
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
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Category not found", 404));

        // Kiểm tra xem đã bị xóa mềm chưa
        if (category.getIsDeleted()) {
             throw new AppException("Category not found (Deleted)", 404);
        }

        return CategoryResponseDTO.fromEntity(category);
    }

    // --- PHẦN BỔ SUNG CRUD ---

    /**
     * Tạo mới Category
     */
    @Transactional
    public CategoryResponseDTO createCategory(CategoryResponseDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription()); 
        category.setIsDeleted(false);
    
        Category savedCategory = categoryRepository.save(category);
        return CategoryResponseDTO.fromEntity(savedCategory);
    }

    /**
     * Cập nhật Category
     */
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryResponseDTO dto) {
        Category existingCategory = categoryRepository.findById(id)
            .orElseThrow(() -> new AppException("Category not found", 404));

        if (existingCategory.getIsDeleted()) {
            throw new AppException("Category has been deleted and cannot be updated", 400);
        }

        existingCategory.setName(dto.getName());
        existingCategory.setDescription(dto.getDescription());
        Category updatedCategory = categoryRepository.save(existingCategory);
        return CategoryResponseDTO.fromEntity(updatedCategory);
}

    /**
     * Xóa Category (Soft Delete)
     * Thay vì xóa khỏi DB, ta set isDeleted = true
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Category not found", 404));

        existingCategory.setIsDeleted(true); // Đánh dấu là đã xóa
        categoryRepository.save(existingCategory);
    }
}


package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.BookResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.Category;
import com.hutech.bookstore.repository.BookRepository;
import com.hutech.bookstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    // ==========================================
    // PHẦN 1: TÌM KIẾM VÀ LỌC (ĐÃ CẬP NHẬT)
    // ==========================================

    /**
     * Lấy danh sách sách với các filter và pagination
     */
    public Map<String, Object> getBooks(
            Integer page,
            Integer limit,
            String search,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            String stock, // Tham số này dùng để lọc trạng thái Active/Inactive hoặc Stock
            String sortBy,
            String sortOrder) {
        
        // 1. Tạo Sort object
        Sort sort = sortOrder != null && sortOrder.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy != null ? sortBy : "createdAt").ascending() 
            : Sort.by(sortBy != null ? sortBy : "createdAt").descending();
        
        // 2. Tạo Pageable
        Pageable pageable;
        if (page != null && limit != null && page > 0 && limit > 0) {
            pageable = PageRequest.of(page - 1, limit, sort);
        } else {
            pageable = PageRequest.of(0, 1000, sort); // Mặc định lấy nhiều nếu không phân trang
        }
        
        // 3. Xử lý logic lọc trạng thái (Active/Inactive) dựa trên tham số 'stock' từ Frontend
        Boolean isActiveFilter = null;
        if ("active".equalsIgnoreCase(stock)) {
            isActiveFilter = true;
        } else if ("inactive".equalsIgnoreCase(stock)) {
            isActiveFilter = false;
        } 
        // Nếu stock = 'inStock' (logic cũ của User page), ta có thể xử lý riêng nếu cần, 
        // nhưng ở đây ta tập trung vào Admin filter (active/inactive)

        // 4. GỌI REPOSITORY MỚI (Xử lý tất cả filter cùng lúc)
        Page<Book> booksPage = bookRepository.findBooksWithFilters(
            search, 
            categoryId, 
            minPrice, 
            maxPrice, 
            isActiveFilter, 
            pageable
        );
        
        // 5. Convert to DTOs
        List<BookResponseDTO> bookDTOs = booksPage.getContent().stream()
            .map(BookResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        // 6. Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("books", bookDTOs);
        
        Map<String, Object> paginationMap = new HashMap<>();
        paginationMap.put("currentPage", booksPage.getNumber() + 1);
        paginationMap.put("totalPages", booksPage.getTotalPages());
        paginationMap.put("totalItems", booksPage.getTotalElements());
        paginationMap.put("totalBooks", booksPage.getTotalElements()); // Giữ lại field cũ cho tương thích
        paginationMap.put("limit", booksPage.getSize());
        
        data.put("pagination", paginationMap);
        
        // Thêm total ở root cho một số logic cũ nếu cần
        data.put("total", booksPage.getTotalElements()); 
        
        return data;
    }

    /**
     * Lấy sách theo ID
     */
    public BookResponseDTO getBookById(Long id) {
        Optional<Book> bookOpt = bookRepository.findByIdAndIsDeletedFalse(id);
        if (bookOpt.isEmpty()) {
            throw new AppException("Book not found", 404);
        }
        return BookResponseDTO.fromEntity(bookOpt.get());
    }

    // ==========================================
    // PHẦN 2: CÁC PHƯƠNG THỨC WRITE (GIỮ NGUYÊN)
    // ==========================================

    /**
     * Tạo sách mới
     */
    @Transactional
    public BookResponseDTO createBook(BookResponseDTO dto) {
        Book book = new Book();
        
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPrice(dto.getPrice());
        book.setDescription(dto.getDescription());
        book.setImageUrl(dto.getImageUrl());
        book.setStock(dto.getStock() != null ? dto.getStock() : 0);
        
        book.setIsDeleted(false);
        book.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        // Xử lý Category
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new AppException("Category not found", 404));
            
            if (category.getIsDeleted()) {
                throw new AppException("Cannot add book to a deleted category", 400);
            }
            book.setCategory(category);
        }

        Book savedBook = bookRepository.save(book);
        return BookResponseDTO.fromEntity(savedBook);
    }

    /**
     * Cập nhật sách
     */
    @Transactional
    public BookResponseDTO updateBook(Long id, BookResponseDTO dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new AppException("Book not found", 404));

        if (book.getIsDeleted()) {
            throw new AppException("Book has been deleted and cannot be updated", 400);
        }

        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPrice(dto.getPrice());
        book.setDescription(dto.getDescription());
        book.setImageUrl(dto.getImageUrl());
        book.setStock(dto.getStock());
        
        if (dto.getIsActive() != null) {
            book.setIsActive(dto.getIsActive());
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new AppException("Category not found", 404));
            
            if (category.getIsDeleted()) {
                throw new AppException("Cannot assign book to a deleted category", 400);
            }
            book.setCategory(category);
        }

        Book updatedBook = bookRepository.save(book);
        return BookResponseDTO.fromEntity(updatedBook);
    }

    /**
     * Xóa sách (Soft Delete)
     */
    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new AppException("Book not found", 404));

        book.setIsDeleted(true); 
        bookRepository.save(book);
    }
}
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
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

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
            String stock,
            String sortBy,
            String sortOrder) {
        
        // Tạo Sort object
        Sort sort = sortOrder != null && sortOrder.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy != null ? sortBy : "createdAt").ascending() 
            : Sort.by(sortBy != null ? sortBy : "createdAt").descending();
        
        // Tạo Pageable
        Pageable pageable;
        if (page != null && limit != null && page > 0 && limit > 0) {
            pageable = PageRequest.of(page - 1, limit, sort);
        } else {
            pageable = Pageable.unpaged();
        }
        
        // Query books dựa trên filters
        Page<Book> booksPage = queryBooks(search, categoryId, minPrice, maxPrice, pageable);
        
        // Filter by stock nếu có
        List<Book> filteredBooks = booksPage.getContent();
        if ("inStock".equalsIgnoreCase(stock)) {
            filteredBooks = filteredBooks.stream()
                .filter(book -> book.getStock() != null && book.getStock() > 0)
                .collect(Collectors.toList());
        }
        
        // Convert to DTOs
        List<BookResponseDTO> bookDTOs = filteredBooks.stream()
            .map(BookResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        // Build response data
        Map<String, Object> data = new HashMap<>();
        if (page != null && limit != null) {
            data.put("books", bookDTOs);
            long totalItems = "inStock".equalsIgnoreCase(stock) 
                ? bookDTOs.size() 
                : booksPage.getTotalElements();
            Map<String, Object> paginationMap = new HashMap<>();
            paginationMap.put("currentPage", booksPage.getNumber() + 1);
            paginationMap.put("totalPages", booksPage.getTotalPages());
            paginationMap.put("totalItems", totalItems);
            paginationMap.put("totalBooks", totalItems);
            paginationMap.put("pageSize", booksPage.getSize());
            paginationMap.put("limit", booksPage.getSize());
            data.put("pagination", paginationMap);
        } else {
            data.put("books", bookDTOs);
            data.put("total", bookDTOs.size());
        }
        
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

    /**
     * Cập nhật sách (Admin)
     */
    @Transactional
    public BookResponseDTO updateBook(Long id, Map<String, Object> updates) {
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new AppException("Book not found", 404));

        if (updates.containsKey("title")) {
            book.setTitle(String.valueOf(updates.get("title")).trim());
        }
        if (updates.containsKey("author")) {
            book.setAuthor(String.valueOf(updates.get("author")).trim());
        }
        if (updates.containsKey("description")) {
            book.setDescription(updates.get("description") != null ? String.valueOf(updates.get("description")).trim() : null);
        }
        if (updates.containsKey("price")) {
            try {
                book.setPrice(Double.parseDouble(String.valueOf(updates.get("price"))));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("stock")) {
            try {
                Integer newStock = Integer.parseInt(String.valueOf(updates.get("stock")));
                book.setStock(newStock);
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("isbn")) {
            book.setIsbn(updates.get("isbn") != null ? String.valueOf(updates.get("isbn")).trim() : null);
        }
        if (updates.containsKey("publisher")) {
            book.setPublisher(updates.get("publisher") != null ? String.valueOf(updates.get("publisher")).trim() : null);
        }
        if (updates.containsKey("publicationDate")) {
            try {
                String pd = String.valueOf(updates.get("publicationDate"));
                if (pd != null && !pd.isBlank()) {
                    book.setPublicationDate(LocalDate.parse(pd));
                }
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("pages")) {
            try {
                book.setPages(Integer.parseInt(String.valueOf(updates.get("pages"))));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("dimensions")) {
            book.setDimensions(updates.get("dimensions") != null ? String.valueOf(updates.get("dimensions")).trim() : null);
        }
        if (updates.containsKey("weight")) {
            try {
                book.setWeight(Double.parseDouble(String.valueOf(updates.get("weight"))));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("fileUrl")) {
            book.setFileUrl(updates.get("fileUrl") != null ? String.valueOf(updates.get("fileUrl")).trim() : null);
        }
        if (updates.containsKey("imageUrl")) {
            book.setImageUrl(updates.get("imageUrl") != null ? String.valueOf(updates.get("imageUrl")).trim() : null);
        }
        if (updates.containsKey("categoryId")) {
            try {
                Long catId = Long.parseLong(String.valueOf(updates.get("categoryId")));
                Category category = categoryRepository.findById(catId)
                    .orElseThrow(() -> new AppException("Category not found", 404));
                book.setCategory(category);
            } catch (Exception ignored) {}
        }

        // Update status based on stock
        if (book.getStock() == null || book.getStock() <= 0) {
            book.setStatus(Book.BookStatus.OUT_OF_STOCK);
            book.setIsActive(false);
        } else {
            book.setStatus(Book.BookStatus.AVAILABLE);
            book.setIsActive(true);
        }

        book = bookRepository.save(book);
        return BookResponseDTO.fromEntity(book);
    }

    /**
     * Tạo sách mới
     */
    @Transactional
    public BookResponseDTO createBook(Map<String, Object> payload) {
        Book book = new Book();

        if (payload.containsKey("title")) {
            book.setTitle(String.valueOf(payload.get("title")).trim());
        } else {
            throw new AppException("Title is required", 400);
        }

        if (payload.containsKey("author")) {
            book.setAuthor(String.valueOf(payload.get("author")).trim());
        }

        if (payload.containsKey("description")) {
            book.setDescription(payload.get("description") != null ? String.valueOf(payload.get("description")).trim() : null);
        } else {
            book.setDescription("");
        }

        if (payload.containsKey("price")) {
            try {
                book.setPrice(Double.parseDouble(String.valueOf(payload.get("price"))));
            } catch (Exception e) {
                throw new AppException("Invalid price", 400);
            }
        } else {
            book.setPrice(0.0);
        }

        if (payload.containsKey("stock")) {
            try {
                book.setStock(Integer.parseInt(String.valueOf(payload.get("stock"))));
            } catch (Exception e) {
                book.setStock(0);
            }
        } else {
            book.setStock(0);
        }

        if (payload.containsKey("isbn")) {
            book.setIsbn(payload.get("isbn") != null ? String.valueOf(payload.get("isbn")).trim() : null);
        }

        if (payload.containsKey("publisher")) {
            book.setPublisher(payload.get("publisher") != null ? String.valueOf(payload.get("publisher")).trim() : null);
        }

        if (payload.containsKey("publicationDate")) {
            try {
                String pd = String.valueOf(payload.get("publicationDate"));
                if (pd != null && !pd.isBlank()) {
                    book.setPublicationDate(LocalDate.parse(pd));
                }
            } catch (Exception ignored) {}
        }

        if (payload.containsKey("pages")) {
            try {
                book.setPages(Integer.parseInt(String.valueOf(payload.get("pages"))));
            } catch (Exception ignored) {}
        }

        if (payload.containsKey("dimensions")) {
            book.setDimensions(payload.get("dimensions") != null ? String.valueOf(payload.get("dimensions")).trim() : null);
        }

        if (payload.containsKey("weight")) {
            try {
                book.setWeight(Double.parseDouble(String.valueOf(payload.get("weight"))));
            } catch (Exception ignored) {}
        }

        if (payload.containsKey("fileUrl")) {
            book.setFileUrl(payload.get("fileUrl") != null ? String.valueOf(payload.get("fileUrl")).trim() : null);
        }

        if (payload.containsKey("imageUrl")) {
            book.setImageUrl(payload.get("imageUrl") != null ? String.valueOf(payload.get("imageUrl")).trim() : null);
        }

        if (payload.containsKey("categoryId")) {
            try {
                Long catId = Long.parseLong(String.valueOf(payload.get("categoryId")));
                Category category = categoryRepository.findById(catId)
                    .orElseThrow(() -> new AppException("Category not found", 404));
                book.setCategory(category);
            } catch (Exception e) {
                throw new AppException("Invalid category", 400);
            }
        } else {
            throw new AppException("Category is required", 400);
        }

        // Set initial status based on stock
        if (book.getStock() == null || book.getStock() <= 0) {
            book.setStatus(Book.BookStatus.OUT_OF_STOCK);
            book.setIsActive(false);
        } else {
            book.setStatus(Book.BookStatus.AVAILABLE);
            book.setIsActive(true);
        }

        Book saved = bookRepository.save(book);
        return BookResponseDTO.fromEntity(saved);
    }

    /**
     * Query books dựa trên các filters
     */
    private Page<Book> queryBooks(
            String search,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {
        
        if (search != null && !search.trim().isEmpty()) {
            return bookRepository.searchBooks(search.trim(), pageable);
        } else if (minPrice != null && maxPrice != null) {
            return bookRepository.findByPriceRange(minPrice, maxPrice, pageable);
        } else if (categoryId != null) {
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            if (categoryOpt.isPresent() && !categoryOpt.get().getIsDeleted()) {
                return bookRepository.findByCategoryAndIsDeletedFalseAndIsActiveTrue(
                    categoryOpt.get(), 
                    pageable
                );
            } else {
                return Page.empty(pageable);
            }
        } else {
            return bookRepository.findByIsDeletedFalseAndIsActiveTrue(pageable);
        }
    }
}


package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // Tìm sách Active (cho User)
    Optional<Book> findByIdAndIsDeletedFalse(Long id);
    Page<Book> findByIsDeletedFalseAndIsActiveTrue(Pageable pageable);
    Page<Book> findByCategoryAndIsDeletedFalseAndIsActiveTrue(Category category, Pageable pageable);
    
    // Tìm TẤT CẢ sách chưa xóa (cho Admin - bao gồm cả isActive=false)
    Page<Book> findByIsDeletedFalse(Pageable pageable);
    Page<Book> findByCategoryAndIsDeletedFalse(Category category, Pageable pageable);

    // Search cho User (Chỉ Active)
    @Query("SELECT b FROM Book b WHERE b.isDeleted = false AND b.isActive = true " +
           "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Book> searchBooksActive(@Param("search") String search, Pageable pageable);

    // Search cho Admin (Tất cả)
    @Query("SELECT b FROM Book b WHERE b.isDeleted = false " +
           "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Book> searchAllBooks(@Param("search") String search, Pageable pageable);
    
    // Filter Price Active
    @Query("SELECT b FROM Book b WHERE b.isDeleted = false AND b.isActive = true " +
           "AND b.price BETWEEN :minPrice AND :maxPrice")
    Page<Book> findByPriceRangeActive(@Param("minPrice") Double minPrice, 
                               @Param("maxPrice") Double maxPrice, 
                               Pageable pageable);

    // Filter Price All
    @Query("SELECT b FROM Book b WHERE b.isDeleted = false " +
           "AND b.price BETWEEN :minPrice AND :maxPrice")
    Page<Book> findByPriceRangeAll(@Param("minPrice") Double minPrice, 
                               @Param("maxPrice") Double maxPrice, 
                               Pageable pageable);
}
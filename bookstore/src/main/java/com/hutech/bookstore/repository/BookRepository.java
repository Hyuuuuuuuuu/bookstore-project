package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIdAndIsDeletedFalse(Long id);
    Page<Book> findByIsDeletedFalseAndIsActiveTrue(Pageable pageable);
    Page<Book> findByCategoryAndIsDeletedFalseAndIsActiveTrue(Category category, Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.isDeleted = false AND b.isActive = true " +
           "AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Book> searchBooks(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE b.isDeleted = false AND b.isActive = true " +
           "AND b.price BETWEEN :minPrice AND :maxPrice")
    Page<Book> findByPriceRange(@Param("minPrice") Double minPrice, 
                               @Param("maxPrice") Double maxPrice, 
                               Pageable pageable);
}


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
    @Query("SELECT b FROM Book b WHERE b.isDeleted = false " +
           "AND (:search IS NULL OR :search = '' OR " +
           "    LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(b.isbn) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:categoryId IS NULL OR b.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR b.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR b.price <= :maxPrice) " +
           "AND (:isActive IS NULL OR b.isActive = :isActive)")
    Page<Book> findBooksWithFilters(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}

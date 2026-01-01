package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Favorite;
import com.hutech.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndBookIdAndIsDeletedFalse(User user, Long bookId);
    List<Favorite> findByUserAndIsFavouriteTrueAndIsDeletedFalse(User user);
    
    // Check if book is favorite for user (without isDeleted check, to find all records)
    @Query("SELECT f FROM Favorite f WHERE f.user = :user AND f.book.id = :bookId")
    Optional<Favorite> findByUserAndBookId(@Param("user") User user, @Param("bookId") Long bookId);
    
    // Count favorites for a book
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.book.id = :bookId AND f.isFavourite = true AND f.isDeleted = false")
    Long countByBookIdAndIsFavouriteTrue(@Param("bookId") Long bookId);
}


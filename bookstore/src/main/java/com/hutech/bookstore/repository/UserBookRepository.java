package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.User;
import com.hutech.bookstore.model.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    List<UserBook> findByUserAndIsActiveTrue(User user);
    Optional<UserBook> findByUserAndBookIdAndIsActiveTrue(User user, Long bookId);
    boolean existsByUserAndBookIdAndIsActiveTrue(User user, Long bookId);
}


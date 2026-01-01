package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    boolean existsByEmail(String email);

    // --- CÂU TRUY VẤN TÌM KIẾM NGƯỜI DÙNG ---
    @Query("SELECT u FROM User u WHERE u.isDeleted = false " +
           "AND (:search IS NULL OR :search = '' OR " +
           "    LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:roleName IS NULL OR u.role.name = :roleName)")
    Page<User> findUsersWithFilters(
            @Param("search") String search,
            @Param("roleName") String roleName,
            Pageable pageable
    );
    // ----------------------------------------
}
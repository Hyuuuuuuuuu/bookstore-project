package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    boolean existsByCode(String code);

    // --- BỔ SUNG DÒNG NÀY ĐỂ SỬA LỖI ORDER SERVICE ---
    Optional<Voucher> findByCodeAndIsDeletedFalse(String code); 
    // -----------------------------------------------

    Optional<Voucher> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT v FROM Voucher v WHERE " +
           "v.isDeleted = false AND " +
           "(:search IS NULL OR :search = '' OR LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR v.isActive = :isActive) " +
           "AND (:type IS NULL OR v.type = :type)")
    Page<Voucher> findVouchersWithFilters(
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            @Param("type") Voucher.VoucherType type,
            Pageable pageable
    );
}
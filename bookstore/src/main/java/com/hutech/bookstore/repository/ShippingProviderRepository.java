package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.ShippingProvider;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, Long> {
    Optional<ShippingProvider> findByCodeAndIsDeletedFalse(String code);
    List<ShippingProvider> findByStatusAndIsDeletedFalse(ShippingProvider.Status status);

    // Query tìm kiếm nâng cao: Search theo tên/mã + Filter theo Status + Sort
    @Query("SELECT s FROM ShippingProvider s WHERE s.isDeleted = false " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR s.status = :status)")
    List<ShippingProvider> searchProviders(@Param("search") String search, 
                                         @Param("status") ShippingProvider.Status status, 
                                         Sort sort);
}
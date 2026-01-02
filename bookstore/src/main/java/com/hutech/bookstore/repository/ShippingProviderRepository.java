package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.ShippingProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, Long> {
    Optional<ShippingProvider> findByCodeAndIsDeletedFalse(String code);
    List<ShippingProvider> findByStatusAndIsDeletedFalse(com.hutech.bookstore.model.ShippingProvider.Status status);
}


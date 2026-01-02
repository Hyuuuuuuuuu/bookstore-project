package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    Page<Order> findByUserAndIsDeletedFalse(User user, Pageable pageable);
    Page<Order> findByIsDeletedFalse(Pageable pageable);
}


package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderAndIsDeletedFalse(Order order);
}


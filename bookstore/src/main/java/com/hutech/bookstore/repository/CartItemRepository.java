package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Cart;
import com.hutech.bookstore.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    /**
     * Tìm cart item theo cart và book
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart = :cart AND ci.book.id = :bookId")
    Optional<CartItem> findByCartAndBookId(@Param("cart") Cart cart, @Param("bookId") Long bookId);

    /**
     * Xóa cart item theo cart và book
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart = :cart AND ci.book.id = :bookId")
    void deleteByCartAndBookId(@Param("cart") Cart cart, @Param("bookId") Long bookId);

    /**
     * Xóa tất cả cart items của cart
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart = :cart")
    void deleteByCart(@Param("cart") Cart cart);
}


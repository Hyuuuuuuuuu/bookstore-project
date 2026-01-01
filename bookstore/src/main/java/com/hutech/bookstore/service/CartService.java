package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.CartItemResponseDTO;
import com.hutech.bookstore.dto.CartResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.Cart;
import com.hutech.bookstore.model.CartItem;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.BookRepository;
import com.hutech.bookstore.repository.CartItemRepository;
import com.hutech.bookstore.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    /**
     * Lấy giỏ hàng của user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserCart(User user) {
        Optional<Cart> cartOpt = cartRepository.findByUserAndIsDeletedFalse(user);
        
        if (cartOpt.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("cart", Map.of(
                "items", List.of(),
                "totalItems", 0,
                "totalPrice", 0.0
            ));
            return data;
        }
        
        Cart cart = cartOpt.get();
        // Force fetch items (vì LAZY loading)
        if (cart.getItems() != null) {
            cart.getItems().size(); // Trigger lazy loading
        }
        cart.calculateTotals(); // Tính lại totals
        
        // Convert to DTOs
        List<CartItemResponseDTO> items = (cart.getItems() != null) 
            ? cart.getItems().stream()
                .map(CartItemResponseDTO::fromEntity)
                .collect(Collectors.toList())
            : List.of();
        
        // Tính totalPrice từ items
        double totalPrice = items.stream()
            .mapToDouble(CartItemResponseDTO::getTotalPrice)
            .sum();
        
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("items", items);
        cartData.put("totalItems", cart.getTotalItems());
        cartData.put("totalPrice", totalPrice);
        
        Map<String, Object> data = new HashMap<>();
        data.put("cart", cartData);
        
        return data;
    }

    /**
     * Thêm sách vào giỏ hàng
     */
    @Transactional
    public Map<String, Object> addToCart(User user, Long bookId, Integer quantity) {
        // Kiểm tra sách có tồn tại không
        Optional<Book> bookOpt = bookRepository.findByIdAndIsDeletedFalse(bookId);
        if (bookOpt.isEmpty()) {
            throw new AppException("Book not found", 404);
        }
        
        Book book = bookOpt.get();
        
        // Kiểm tra tồn kho
        if (book.getStock() < quantity) {
            throw new AppException("Insufficient stock", 400);
        }
        
        // Lấy hoặc tạo cart
        Cart cart = cartRepository.findByUserAndIsDeletedFalse(user)
            .orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setUser(user);
                newCart.setItems(new java.util.ArrayList<>());
                return cartRepository.save(newCart);
            });
        
        // Kiểm tra sách đã có trong cart chưa
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndBookId(cart, bookId);
        
        if (existingItemOpt.isPresent()) {
            // Cập nhật quantity
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
            if (book.getStock() < newQuantity) {
                throw new AppException("Insufficient stock", 400);
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            // Tạo cart item mới
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setBook(book);
            newItem.setQuantity(quantity);
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }
        
        // Tính lại totals và lưu cart
        cart.calculateTotals();
        cartRepository.save(cart);
        
        // Reload cart với items - force fetch items từ database
        cart = cartRepository.findByUserAndIsDeletedFalse(user).orElse(cart);
        // Force fetch items (vì LAZY loading)
        if (cart != null && cart.getItems() != null) {
            cart.getItems().size(); // Trigger lazy loading
        }
        
        // Convert to DTOs
        List<CartItemResponseDTO> items = (cart != null && cart.getItems() != null) 
            ? cart.getItems().stream()
                .map(CartItemResponseDTO::fromEntity)
                .collect(Collectors.toList())
            : List.of();
        
        double totalPrice = items.stream()
            .mapToDouble(CartItemResponseDTO::getTotalPrice)
            .sum();
        
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("items", items);
        cartData.put("totalItems", cart.getTotalItems());
        cartData.put("totalPrice", totalPrice);
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Book added to cart successfully");
        data.put("cart", cartData);
        
        return data;
    }

    /**
     * Cập nhật số lượng sách trong giỏ hàng
     */
    @Transactional
    public Map<String, Object> updateCartItem(User user, Long bookId, Integer quantity) {
        if (quantity < 0) {
            throw new AppException("Quantity cannot be negative", 400);
        }
        
        // Kiểm tra sách có tồn tại không
        Optional<Book> bookOpt = bookRepository.findByIdAndIsDeletedFalse(bookId);
        if (bookOpt.isEmpty()) {
            throw new AppException("Book not found", 404);
        }
        
        Book book = bookOpt.get();
        
        // Kiểm tra tồn kho nếu quantity > 0
        if (quantity > 0 && book.getStock() < quantity) {
            throw new AppException("Insufficient stock", 400);
        }
        
        // Lấy cart
        Cart cart = cartRepository.findByUserAndIsDeletedFalse(user)
            .orElseThrow(() -> new AppException("Cart not found", 404));
        
        // Tìm cart item
        Optional<CartItem> itemOpt = cartItemRepository.findByCartAndBookId(cart, bookId);
        if (itemOpt.isEmpty()) {
            throw new AppException("Item not found in cart", 404);
        }
        
        CartItem item = itemOpt.get();
        
        if (quantity == 0) {
            // Xóa item nếu quantity = 0
            cartItemRepository.delete(item);
            // Remove from cart's items list
            if (cart.getItems() != null) {
                cart.getItems().removeIf(ci -> ci.getId().equals(item.getId()));
            }
        } else {
            // Cập nhật quantity
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        
        // Tính lại totals và lưu cart
        cart.calculateTotals();
        cartRepository.save(cart);
        
        // Reload cart với items - force fetch items từ database
        cart = cartRepository.findByUserAndIsDeletedFalse(user).orElse(cart);
        // Force fetch items (vì LAZY loading)
        if (cart != null && cart.getItems() != null) {
            cart.getItems().size(); // Trigger lazy loading
        }
        
        // Convert to DTOs
        List<CartItemResponseDTO> items = (cart != null && cart.getItems() != null) 
            ? cart.getItems().stream()
                .map(CartItemResponseDTO::fromEntity)
                .collect(Collectors.toList())
            : List.of();
        
        double totalPrice = items.stream()
            .mapToDouble(CartItemResponseDTO::getTotalPrice)
            .sum();
        
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("items", items);
        cartData.put("totalItems", cart.getTotalItems());
        cartData.put("totalPrice", totalPrice);
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Cart item updated successfully");
        data.put("cart", cartData);
        
        return data;
    }

    /**
     * Xóa sách khỏi giỏ hàng
     */
    @Transactional
    public Map<String, Object> removeFromCart(User user, Long bookId) {
        // Lấy cart
        Cart cart = cartRepository.findByUserAndIsDeletedFalse(user)
            .orElseThrow(() -> new AppException("Cart not found", 404));
        
        // Tìm và xóa cart item
        Optional<CartItem> itemOpt = cartItemRepository.findByCartAndBookId(cart, bookId);
        if (itemOpt.isEmpty()) {
            throw new AppException("Item not found in cart", 404);
        }
        
        CartItem item = itemOpt.get();
        cartItemRepository.delete(item);
        // Remove from cart's items list
        if (cart.getItems() != null) {
            cart.getItems().removeIf(ci -> ci.getId().equals(item.getId()));
        }
        
        // Tính lại totals và lưu cart
        cart.calculateTotals();
        cartRepository.save(cart);
        
        // Reload cart với items - force fetch items từ database
        cart = cartRepository.findByUserAndIsDeletedFalse(user).orElse(cart);
        // Force fetch items (vì LAZY loading)
        if (cart != null && cart.getItems() != null) {
            cart.getItems().size(); // Trigger lazy loading
        }
        
        // Convert to DTOs
        List<CartItemResponseDTO> items = (cart != null && cart.getItems() != null) 
            ? cart.getItems().stream()
                .map(CartItemResponseDTO::fromEntity)
                .collect(Collectors.toList())
            : List.of();
        
        double totalPrice = items.stream()
            .mapToDouble(CartItemResponseDTO::getTotalPrice)
            .sum();
        
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("items", items);
        cartData.put("totalItems", cart.getTotalItems());
        cartData.put("totalPrice", totalPrice);
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Book removed from cart successfully");
        data.put("cart", cartData);
        
        return data;
    }

    /**
     * Xóa tất cả sách khỏi giỏ hàng
     */
    @Transactional
    public Map<String, Object> clearCart(User user) {
        // Lấy cart
        Cart cart = cartRepository.findByUserAndIsDeletedFalse(user)
            .orElseThrow(() -> new AppException("Cart not found", 404));
        
        // Xóa tất cả items
        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
        }
        
        // Tính lại totals và lưu cart
        cart.calculateTotals();
        cartRepository.save(cart);
        
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("items", List.of());
        cartData.put("totalItems", 0);
        cartData.put("totalPrice", 0.0);
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Cart cleared successfully");
        data.put("cart", cartData);
        
        return data;
    }

    /**
     * Kiểm tra sách có trong giỏ hàng không
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkCartItem(User user, Long bookId) {
        Optional<Cart> cartOpt = cartRepository.findByUserAndIsDeletedFalse(user);
        
        if (cartOpt.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("inCart", false);
            data.put("quantity", 0);
            return data;
        }
        
        Cart cart = cartOpt.get();
        Optional<CartItem> itemOpt = cartItemRepository.findByCartAndBookId(cart, bookId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("inCart", itemOpt.isPresent());
        data.put("quantity", itemOpt.map(CartItem::getQuantity).orElse(0));
        
        return data;
    }
}


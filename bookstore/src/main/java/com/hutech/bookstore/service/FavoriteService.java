package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.FavoriteResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.Favorite;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.BookRepository;
import com.hutech.bookstore.repository.FavoriteRepository;
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
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;

    /**
     * Thêm sách vào danh sách yêu thích của user
     */
    @Transactional
    public FavoriteResponseDTO addToFavorites(User user, Long bookId) {
        // Kiểm tra sách có tồn tại không
        Optional<Book> bookOpt = bookRepository.findByIdAndIsDeletedFalse(bookId);
        if (bookOpt.isEmpty()) {
            throw new AppException("Book not found", 404);
        }
        
        Book book = bookOpt.get();
        
        // Kiểm tra sách đã có trong favorites chưa
        Optional<Favorite> existingFavoriteOpt = favoriteRepository.findByUserAndBookId(user, bookId);
        
        Favorite favorite;
        if (existingFavoriteOpt.isPresent()) {
            Favorite existingFavorite = existingFavoriteOpt.get();
            if (existingFavorite.getIsFavourite()) {
                throw new AppException("Book already in favorites", 400);
            }
            // Cập nhật favorite hiện có
            existingFavorite.setIsFavourite(true);
            existingFavorite.setIsDeleted(false);
            favorite = favoriteRepository.save(existingFavorite);
        } else {
            // Tạo favorite mới
            favorite = new Favorite();
            favorite.setUser(user);
            favorite.setBook(book);
            favorite.setIsFavourite(true);
            favorite.setIsDeleted(false);
            favorite = favoriteRepository.save(favorite);
        }
        
        return FavoriteResponseDTO.fromEntity(favorite);
    }

    /**
     * Xóa sách khỏi danh sách yêu thích của user
     */
    @Transactional
    public FavoriteResponseDTO removeFromFavorites(User user, Long bookId) {
        // Kiểm tra sách có tồn tại không
        Optional<Book> bookOpt = bookRepository.findByIdAndIsDeletedFalse(bookId);
        if (bookOpt.isEmpty()) {
            throw new AppException("Book not found", 404);
        }
        
        // Tìm favorite record
        Optional<Favorite> favoriteOpt = favoriteRepository.findByUserAndBookId(user, bookId);
        if (favoriteOpt.isEmpty() || !favoriteOpt.get().getIsFavourite()) {
            throw new AppException("Book not in favorites", 400);
        }
        
        Favorite favorite = favoriteOpt.get();
        favorite.setIsFavourite(false);
        favorite = favoriteRepository.save(favorite);
        
        return FavoriteResponseDTO.fromEntity(favorite);
    }

    /**
     * Lấy danh sách favorites của user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserFavorites(User user) {
        // Lấy danh sách favorites với isFavourite = true
        List<Favorite> favorites = favoriteRepository.findByUserAndIsFavouriteTrueAndIsDeletedFalse(user);
        
        // Convert to DTOs
        List<FavoriteResponseDTO> favoriteDTOs = favorites.stream()
            .map(FavoriteResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("favorites", favoriteDTOs);
        data.put("total", favoriteDTOs.size());
        
        return data;
    }

    /**
     * Kiểm tra sách có trong favorites của user không
     */
    @Transactional(readOnly = true)
    public boolean checkFavorite(User user, Long bookId) {
        Optional<Favorite> favoriteOpt = favoriteRepository.findByUserAndBookId(user, bookId);
        return favoriteOpt.isPresent() && favoriteOpt.get().getIsFavourite();
    }
}


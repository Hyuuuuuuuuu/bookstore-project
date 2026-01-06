package com.hutech.bookstore.service;

import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.model.OrderItem;
import com.hutech.bookstore.model.UserBook;
import com.hutech.bookstore.repository.BookRepository;
import com.hutech.bookstore.repository.OrderItemRepository;
import com.hutech.bookstore.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final UserBookRepository userBookRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookRepository bookRepository;

    /**
     * Thêm ebook/audiobook vào library của user khi order được thanh toán
     */
    @Transactional
    public void addDigitalBooksToLibrary(Order order) {
        // Chỉ xử lý khi order đã được thanh toán
        if (order.getPaymentStatus() != Order.PaymentStatus.COMPLETED) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderAndIsDeletedFalse(order);
        
        for (OrderItem orderItem : orderItems) {
            Book book = orderItem.getBook();
            
            // Chỉ xử lý ebook và audiobook
            if (book.getFormat() == Book.BookFormat.EBOOK || book.getFormat() == Book.BookFormat.AUDIOBOOK) {
                // Kiểm tra xem user đã có sách này trong library chưa
                boolean alreadyExists = userBookRepository.existsByUserAndBookIdAndIsActiveTrue(
                    order.getUser(), book.getId());
                
                if (!alreadyExists) {
                    // Tạo UserBook mới
                    UserBook userBook = new UserBook();
                    userBook.setUser(order.getUser());
                    userBook.setBook(book);
                    userBook.setOrder(order);
                    
                    // Map BookFormat sang UserBook.BookType
                    if (book.getFormat() == Book.BookFormat.EBOOK) {
                        userBook.setBookType(UserBook.BookType.EBOOK);
                    } else if (book.getFormat() == Book.BookFormat.AUDIOBOOK) {
                        userBook.setBookType(UserBook.BookType.AUDIOBOOK);
                    }
                    
                    // Copy thông tin file từ Book
                    if (book.getDigitalFile() != null) {
                        userBook.setFilePath(book.getDigitalFile().getFilePath());
                        userBook.setFileSize(book.getDigitalFile().getFileSize());
                        userBook.setMimeType(book.getDigitalFile().getMimeType());
                    } else if (book.getFileUrl() != null) {
                        userBook.setFilePath(book.getFileUrl());
                    }
                    
                    userBook.setDownloadCount(0);
                    userBook.setIsActive(true);
                    
                    userBookRepository.save(userBook);
                }
            }
        }
    }
}


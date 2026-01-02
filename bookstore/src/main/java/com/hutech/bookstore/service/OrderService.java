package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.CreateOrderRequest;
import com.hutech.bookstore.dto.OrderItemResponseDTO;
import com.hutech.bookstore.dto.OrderResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.*;
import com.hutech.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookRepository bookRepository;
    private final AddressRepository addressRepository;
    private final ShippingProviderRepository shippingProviderRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    /**
     * Tạo đơn hàng mới
     */
    @Transactional
    public OrderResponseDTO createOrder(User user, CreateOrderRequest request) {
        // Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException("No items selected for order", 400);
        }

        // Validate shipping address
        if (request.getShippingAddressId() == null) {
            throw new AppException("Shipping address is required", 400);
        }

        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .filter(addr -> addr.getUser().getId().equals(user.getId()) && !addr.getIsDeleted())
                .orElseThrow(() -> new AppException("Shipping address not found or access denied", 404));

        // Validate shipping provider
        if (request.getShippingProviderId() == null) {
            throw new AppException("Shipping provider is required", 400);
        }

        ShippingProvider shippingProvider = shippingProviderRepository.findById(request.getShippingProviderId())
                .filter(provider -> provider.getStatus() == ShippingProvider.Status.ACTIVE && !provider.getIsDeleted())
                .orElseThrow(() -> new AppException("Selected shipping provider not found or inactive", 400));

        // Tính tổng tiền và kiểm tra tồn kho
        double totalAmount = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();
        List<Long> categoryIds = new ArrayList<>();
        List<Long> bookIds = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Book book = bookRepository.findByIdAndIsDeletedFalse(itemRequest.getBookId())
                    .orElseThrow(() -> new AppException("Book with ID " + itemRequest.getBookId() + " not found", 404));

            if (book.getStock() < itemRequest.getQuantity()) {
                throw new AppException("Insufficient stock for book: " + book.getTitle(), 400);
            }

            double itemTotal = book.getPrice() * itemRequest.getQuantity();
            totalAmount += itemTotal;

            // Collect category and book IDs for voucher validation
            if (book.getCategory() != null) {
                categoryIds.add(book.getCategory().getId());
            }
            bookIds.add(itemRequest.getBookId());
        }

        // Tính shipping fee trước (cần cho voucher FREE_SHIPPING)
        double shippingFee = shippingProvider.getBaseFee();

        // Xử lý voucher nếu có
        double discountAmount = 0.0;
        Voucher voucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            voucher = voucherRepository.findByCodeAndIsDeletedFalse(request.getVoucherCode().trim())
                    .orElseThrow(() -> new AppException("Voucher not found", 404));

            // Validate voucher (đơn giản hóa - có thể mở rộng sau)
            if (!voucher.getIsActive()) {
                throw new AppException("Voucher is not active", 400);
            }

            LocalDateTime now = LocalDateTime.now();
            if (voucher.getValidFrom() != null && voucher.getValidFrom().isAfter(now)) {
                throw new AppException("Voucher is not yet valid", 400);
            }
            if (voucher.getValidTo() != null && voucher.getValidTo().isBefore(now)) {
                throw new AppException("Voucher has expired", 400);
            }

            // Kiểm tra min order amount
            if (voucher.getMinOrderAmount() != null && totalAmount < voucher.getMinOrderAmount()) {
                throw new AppException("Order amount must be at least " + voucher.getMinOrderAmount(), 400);
            }

            // Kiểm tra usage limit
            if (voucher.getUsageLimit() != null) {
                long usedCount = voucherUsageRepository.countByVoucherId(voucher.getId());
                if (usedCount >= voucher.getUsageLimit()) {
                    throw new AppException("Voucher usage limit exceeded", 400);
                }
            }

            // Kiểm tra user đã dùng voucher này chưa (nếu voucher chỉ dùng 1 lần/user)
            if (voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), user.getId())) {
                throw new AppException("You have already used this voucher", 400);
            }

            // Tính discount
            if (voucher.getType() == Voucher.VoucherType.PERCENTAGE) {
                discountAmount = totalAmount * voucher.getValue() / 100.0;
                if (voucher.getMaxDiscountAmount() != null && discountAmount > voucher.getMaxDiscountAmount()) {
                    discountAmount = voucher.getMaxDiscountAmount();
                }
            } else if (voucher.getType() == Voucher.VoucherType.FIXED_AMOUNT) {
                discountAmount = voucher.getValue();
            } else if (voucher.getType() == Voucher.VoucherType.FREE_SHIPPING) {
                discountAmount = shippingFee; // Free shipping = discount bằng shipping fee
            }

            // Đảm bảo discount không vượt quá totalAmount
            if (discountAmount > totalAmount) {
                discountAmount = totalAmount;
            }
        }

        // Tính final amount sau khi áp dụng voucher
        double finalAmount = totalAmount - discountAmount;

        // Cập nhật tổng tiền bao gồm phí ship
        double finalAmountWithShipping = finalAmount + shippingFee;

        // Tạo mã đơn hàng
        String orderCode = generateOrderCode();

        // Tạo đơn hàng
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setUser(user);
        order.setTotalPrice(finalAmountWithShipping);
        order.setOriginalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setVoucher(voucher);
        order.setShippingAddress(shippingAddress);
        order.setShippingProvider(shippingProvider);
        order.setShippingFee(shippingFee);
        order.setPaymentMethod(Order.PaymentMethod.valueOf(
                request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "COD"));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order = orderRepository.save(order);

        // Sử dụng voucher nếu có
        if (voucher != null) {
            VoucherUsage voucherUsage = new VoucherUsage();
            voucherUsage.setVoucher(voucher);
            voucherUsage.setUser(user);
            voucherUsage.setOrder(order);
            voucherUsage.setVoucherCode(voucher.getCode());
            voucherUsage.setDiscountAmount(discountAmount);
            voucherUsage.setOrderAmount(totalAmount);
            voucherUsageRepository.save(voucherUsage);

            // Cập nhật usedCount của voucher
            voucher.setUsedCount((voucher.getUsedCount() != null ? voucher.getUsedCount() : 0) + 1);
            voucherRepository.save(voucher);
        }

        // Tạo OrderItem records
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Book book = bookRepository.findByIdAndIsDeletedFalse(itemRequest.getBookId()).orElse(null);
            if (book != null) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setBook(book);
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setPriceAtPurchase(book.getPrice());
                orderItems.add(orderItemRepository.save(orderItem));

                // Cập nhật stock cho sách vật lý
                if (book.getFormat() != Book.BookFormat.EBOOK && book.getFormat() != Book.BookFormat.AUDIOBOOK) {
                    book.setStock(book.getStock() - itemRequest.getQuantity());
                    // Update status based on stock
                    if (book.getStock() <= 0) {
                        book.setStatus(Book.BookStatus.OUT_OF_STOCK);
                        book.setIsActive(false);
                    } else {
                        book.setStatus(Book.BookStatus.AVAILABLE);
                        book.setIsActive(true);
                    }
                    bookRepository.save(book);
                }
            }
        }

        // Tạo Payment record
        try {
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(order.getTotalPrice());

            // Safely convert payment method enum
            Payment.PaymentMethod paymentMethod;
            try {
                paymentMethod = Payment.PaymentMethod.valueOf(order.getPaymentMethod().name());
            } catch (IllegalArgumentException e) {
                // Default to COD if the payment method is not supported in Payment enum
                paymentMethod = Payment.PaymentMethod.COD;
                System.err
                        .println("Payment method not supported, defaulting to COD: " + order.getPaymentMethod().name());
            }

            payment.setMethod(paymentMethod);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setDescription("Payment for order " + order.getOrderCode());
            paymentRepository.save(payment);
        } catch (Exception e) {
            // Log error nhưng không throw để không ảnh hưởng đến việc tạo đơn hàng
            System.err.println("Failed to create payment record: " + e.getMessage());
        }

        // Xóa items khỏi cart - không dùng transaction để tránh rollback-only
        try {
            for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                // Xóa trực tiếp từ repository để tránh transaction lồng nhau
                Cart cart = cartRepository.findByUserAndIsDeletedFalse(user)
                        .orElse(null);
                if (cart != null) {
                    Optional<CartItem> itemOpt = cartItemRepository.findByCartAndBookId(cart, itemRequest.getBookId());
                    if (itemOpt.isPresent()) {
                        CartItem item = itemOpt.get();
                        cartItemRepository.delete(item);
                        if (cart.getItems() != null) {
                            cart.getItems().removeIf(ci -> ci.getId().equals(item.getId()));
                        }
                        cart.calculateTotals();
                        cartRepository.save(cart);
                    }
                }
            }
        } catch (Exception e) {
            // Log error nhưng không throw
            System.err.println("Failed to remove items from cart: " + e.getMessage());
        }

        // Convert to DTO
        List<OrderItemResponseDTO> orderItemDTOs = orderItems.stream()
                .map(OrderItemResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return OrderResponseDTO.fromEntityWithItems(order, orderItemDTOs);
    }

    /**
     * Lấy danh sách đơn hàng của user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserOrders(User user, Integer page, Integer limit, String status) {
        Pageable pageable = PageRequest.of(
                page != null && page > 0 ? page - 1 : 0,
                limit != null && limit > 0 ? limit : 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> ordersPage;
        if (status != null && !status.trim().isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                ordersPage = orderRepository.findByUserAndIsDeletedFalse(user, pageable);
                // Filter by status manually since we can't add status filter to repository
                // query easily
                List<Order> filteredOrders = ordersPage.getContent().stream()
                        .filter(order -> order.getStatus() == orderStatus)
                        .collect(Collectors.toList());
                // Create new page with filtered content
                ordersPage = new org.springframework.data.domain.PageImpl<>(
                        filteredOrders, pageable, filteredOrders.size());
            } catch (IllegalArgumentException e) {
                ordersPage = Page.empty(pageable);
            }
        } else {
            ordersPage = orderRepository.findByUserAndIsDeletedFalse(user, pageable);
        }

        // Convert to DTOs with items
        List<OrderResponseDTO> orderDTOs = ordersPage.getContent().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderAndIsDeletedFalse(order);
                    List<OrderItemResponseDTO> itemDTOs = items.stream()
                            .map(OrderItemResponseDTO::fromEntity)
                            .collect(Collectors.toList());
                    return OrderResponseDTO.fromEntityWithItems(order, itemDTOs);
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("orders", orderDTOs);
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", ordersPage.getNumber() + 1);
        pagination.put("limit", ordersPage.getSize());
        pagination.put("total", ordersPage.getTotalElements());
        pagination.put("pages", ordersPage.getTotalPages());
        data.put("pagination", pagination);

        return data;
    }

    /**
     * Lấy tất cả đơn hàng (Admin only)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllOrders(Integer page, Integer limit, String status, Long userId) {
        Pageable pageable = PageRequest.of(
                page != null && page > 0 ? page - 1 : 0,
                limit != null && limit > 0 ? limit : 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> ordersPage = orderRepository.findByIsDeletedFalse(pageable);

        // Filter by status and userId if provided
        List<Order> filteredOrders = ordersPage.getContent();
        if (status != null && !status.trim().isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                filteredOrders = filteredOrders.stream()
                        .filter(order -> order.getStatus() == orderStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                filteredOrders = List.of();
            }
        }
        if (userId != null) {
            filteredOrders = filteredOrders.stream()
                    .filter(order -> order.getUser().getId().equals(userId))
                    .collect(Collectors.toList());
        }

        // Convert to DTOs with items
        List<OrderResponseDTO> orderDTOs = filteredOrders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderAndIsDeletedFalse(order);
                    List<OrderItemResponseDTO> itemDTOs = items.stream()
                            .map(OrderItemResponseDTO::fromEntity)
                            .collect(Collectors.toList());
                    return OrderResponseDTO.fromEntityWithItems(order, itemDTOs);
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("orders", orderDTOs);
        data.put("total", orderDTOs.size());
        if (page != null && limit != null) {
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("limit", limit);
            pagination.put("total", orderDTOs.size());
            pagination.put("pages", (int) Math.ceil((double) orderDTOs.size() / limit));
            data.put("pagination", pagination);
        }

        return data;
    }

    /**
     * Lấy chi tiết đơn hàng
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(User user, Long orderId, boolean isAdmin) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || orderOpt.get().getIsDeleted()) {
            throw new AppException("Order not found", 404);
        }

        Order order = orderOpt.get();

        // Kiểm tra quyền truy cập
        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
            throw new AppException("Access denied", 403);
        }

        // Lấy order items
        List<OrderItem> items = orderItemRepository.findByOrderAndIsDeletedFalse(order);
        List<OrderItemResponseDTO> itemDTOs = items.stream()
                .map(OrderItemResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return OrderResponseDTO.fromEntityWithItems(order, itemDTOs);
    }

    /**
     * Hủy đơn hàng
     */
    @Transactional
    public OrderResponseDTO cancelOrder(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser().getId().equals(user.getId()) && !o.getIsDeleted())
                .orElseThrow(() -> new AppException("Order not found", 404));

        // Chỉ cho phép hủy đơn hàng ở trạng thái pending hoặc confirmed
        if (order.getStatus() != Order.OrderStatus.PENDING &&
                order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new AppException(
                    "Cannot cancel order in current status. Only pending and confirmed orders can be cancelled.", 400);
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // Hoàn lại stock cho sách vật lý
        List<OrderItem> orderItems = orderItemRepository.findByOrderAndIsDeletedFalse(order);
        for (OrderItem item : orderItems) {
            Book book = item.getBook();
            if (book.getFormat() != Book.BookFormat.EBOOK &&
                    book.getFormat() != Book.BookFormat.AUDIOBOOK) {
                book.setStock(book.getStock() + item.getQuantity());
                    // Update status based on restored stock
                    if (book.getStock() <= 0) {
                        book.setStatus(Book.BookStatus.OUT_OF_STOCK);
                        book.setIsActive(false);
                    } else {
                        book.setStatus(Book.BookStatus.AVAILABLE);
                        book.setIsActive(true);
                    }
                    bookRepository.save(book);
            }
        }

        // Convert to DTO
        List<OrderItemResponseDTO> itemDTOs = orderItems.stream()
                .map(OrderItemResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return OrderResponseDTO.fromEntityWithItems(order, itemDTOs);
    }

    /**
     * Cập nhật trạng thái đơn hàng (Admin only)
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> !o.getIsDeleted())
                .orElseThrow(() -> new AppException("Order not found", 404));

        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());

            // Enforce allowed transitions and update timestamps.
            Order.OrderStatus current = order.getStatus();

            // Define allowed transitions for admin updates
            Map<Order.OrderStatus, List<Order.OrderStatus>> allowedTransitions = Map.of(
                    Order.OrderStatus.PENDING, List.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.CANCELLED),
                    Order.OrderStatus.CONFIRMED, List.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.CANCELLED),
                    Order.OrderStatus.SHIPPED, List.of(Order.OrderStatus.DELIVERED),
                    Order.OrderStatus.DELIVERED, List.of(Order.OrderStatus.DELIVERED),
                    Order.OrderStatus.CANCELLED, List.of(Order.OrderStatus.CANCELLED)
            );

            if (!allowedTransitions.getOrDefault(current, List.of()).contains(orderStatus)) {
                throw new AppException("Invalid status transition from " + current + " to " + orderStatus, 400);
            }

            // Apply status change and timestamps
            order.setStatus(orderStatus);
            switch (orderStatus) {
                case CONFIRMED -> order.setConfirmedAt(LocalDateTime.now());
                case SHIPPED -> order.setShippedAt(LocalDateTime.now());
                case DELIVERED -> {
                    order.setDeliveredAt(LocalDateTime.now());
                    if (order.getPaymentStatus() == Order.PaymentStatus.PENDING) {
                        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
                        order.setPaidAt(LocalDateTime.now());
                    }
                }
                case CANCELLED -> {
                    order.setCancelledAt(LocalDateTime.now());
                    // When admin cancels an order, restore stock for physical books similar to user cancel
                    List<OrderItem> orderItems = orderItemRepository.findByOrderAndIsDeletedFalse(order);
                    for (OrderItem item : orderItems) {
                        Book book = item.getBook();
                        if (book.getFormat() != Book.BookFormat.EBOOK &&
                                book.getFormat() != Book.BookFormat.AUDIOBOOK) {
                            book.setStock(book.getStock() + item.getQuantity());
                            if (book.getStock() <= 0) {
                                book.setStatus(Book.BookStatus.OUT_OF_STOCK);
                                book.setIsActive(false);
                            } else {
                                book.setStatus(Book.BookStatus.AVAILABLE);
                                book.setIsActive(true);
                            }
                            bookRepository.save(book);
                        }
                    }
                }
                default -> {
                }
            }

            order = orderRepository.save(order);

            // Convert to DTO
            List<OrderItem> items = orderItemRepository.findByOrderAndIsDeletedFalse(order);
            List<OrderItemResponseDTO> itemDTOs = items.stream()
                    .map(OrderItemResponseDTO::fromEntity)
                    .collect(Collectors.toList());

            return OrderResponseDTO.fromEntityWithItems(order, itemDTOs);
        } catch (IllegalArgumentException e) {
            throw new AppException("Invalid status: " + status, 400);
        }
    }

    /**
     * Tạo mã đơn hàng: ORD-YYYYMMDD-RANDOM4
     */
    private String generateOrderCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random4 = new Random().nextInt(9000) + 1000; // 1000-9999
        String orderCode = "ORD-" + dateStr + "-" + random4;

        // Kiểm tra unique, nếu không unique thì thử lại
        int attempts = 0;
        while (orderRepository.findByOrderCode(orderCode).isPresent() && attempts < 10) {
            random4 = new Random().nextInt(9000) + 1000;
            orderCode = "ORD-" + dateStr + "-" + random4;
            attempts++;
        }

        // Nếu vẫn không unique, thêm timestamp
        if (orderRepository.findByOrderCode(orderCode).isPresent()) {
            long timestamp = System.currentTimeMillis();
            orderCode = "ORD-" + dateStr + "-" + (timestamp % 10000);
        }

        return orderCode;
    }
}

package com.hutech.bookstore.service;

import com.hutech.bookstore.model.Book;
import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.repository.BookRepository;
import com.hutech.bookstore.repository.OrderRepository;
import com.hutech.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(String range) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime previousStart;
        LocalDateTime previousEnd;

        // Determine date range
        switch (range) {
            case "7days":
                start = end.minusDays(7);
                previousEnd = start;
                previousStart = previousEnd.minusDays(7);
                break;
            case "90days":
                start = end.minusDays(90);
                previousEnd = start;
                previousStart = previousEnd.minusDays(90);
                break;
            case "1year":
                start = end.minusYears(1);
                previousEnd = start;
                previousStart = previousEnd.minusYears(1);
                break;
            case "30days":
            default:
                start = end.minusDays(30);
                previousEnd = start;
                previousStart = previousEnd.minusDays(30);
                break;
        }

        Map<String, Object> data = new HashMap<>();

        // 1. Sales Report
        Double currentRevenue = orderRepository.sumRevenueBetween(start, end);
        if (currentRevenue == null) currentRevenue = 0.0;
        
        Double previousRevenue = orderRepository.sumRevenueBetween(previousStart, previousEnd);
        if (previousRevenue == null) previousRevenue = 0.0;

        double revenueGrowth = calculateGrowth(currentRevenue, previousRevenue);

        Map<String, Object> sales = new HashMap<>();
        sales.put("total", currentRevenue);
        sales.put("growth", Math.round(revenueGrowth * 100.0) / 100.0);
        data.put("sales", sales);

        // 2. Orders Report
        long currentOrders = orderRepository.countByCreatedAtBetween(start, end);
        long previousOrders = orderRepository.countByCreatedAtBetween(previousStart, previousEnd);
        double orderGrowth = calculateGrowth((double) currentOrders, (double) previousOrders);

        long completedOrders = orderRepository.countByStatusAndCreatedAtBetween(Order.OrderStatus.DELIVERED, start, end);
        long pendingOrders = orderRepository.countByStatusAndCreatedAtBetween(Order.OrderStatus.PENDING, start, end);
        long cancelledOrders = orderRepository.countByStatusAndCreatedAtBetween(Order.OrderStatus.CANCELLED, start, end);

        Map<String, Object> orders = new HashMap<>();
        orders.put("total", currentOrders);
        orders.put("growth", Math.round(orderGrowth * 100.0) / 100.0);
        orders.put("status", Map.of(
            "completed", completedOrders,
            "pending", pendingOrders,
            "cancelled", cancelledOrders
        ));
        data.put("orders", orders);

        // 3. Customers Report
        long newCustomers = userRepository.countByCreatedAtBetween(start, end);
        long totalCustomers = userRepository.count();
        
        Map<String, Object> customers = new HashMap<>();
        customers.put("new", newCustomers);
        customers.put("total", totalCustomers);
        data.put("customers", customers);

        // 4. Top Books (Simple logic: based on view count or stock movement, ideal would be order items query)
        // For simplicity, returning books with highest price or view count here
        // In a real app, query OrderItems table grouped by Book
        List<Book> topBooksList = bookRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "viewCount"))).getContent();
        List<Map<String, Object>> topBooks = topBooksList.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", b.getTitle());
            map.put("sales", b.getViewCount()); // Using viewCount as proxy for popularity
            map.put("revenue", b.getPrice());
            return map;
        }).collect(Collectors.toList());
        
        data.put("topBooks", topBooks);

        return data;
    }

    private double calculateGrowth(Double current, Double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100.0;
    }
}
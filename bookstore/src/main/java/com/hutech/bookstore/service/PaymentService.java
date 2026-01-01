package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.PaymentResponseDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.model.Payment;
import com.hutech.bookstore.repository.OrderRepository;
import com.hutech.bookstore.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Tạo payment cho COD
     */
    @Transactional
    public PaymentResponseDTO createCODPayment(Long orderId, Double amount, String description) {
        Order order = orderRepository.findById(orderId)
            .filter(o -> !o.getIsDeleted())
            .orElseThrow(() -> new AppException("Order not found", 404));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount != null ? amount : order.getTotalPrice());
        payment.setMethod(Payment.PaymentMethod.COD);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setDescription(description != null ? description : "Payment for order " + order.getOrderCode());
        
        // Generate transaction code
        payment.setTransactionCode(generateTransactionCode());
        
        payment = paymentRepository.save(payment);
        
        return PaymentResponseDTO.fromEntity(payment);
    }

    /**
     * Lấy danh sách payments (Admin)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPayments(Integer page, Integer limit, String status, String method) {
        Pageable pageable = PageRequest.of(
            page != null && page > 0 ? page - 1 : 0,
            limit != null && limit > 0 ? limit : 10,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Filter logic có thể được thêm vào repository nếu cần
        Page<Payment> paymentsPage = paymentRepository.findAll(pageable);
        
        // Filter by status and method if provided
        List<Payment> filteredPayments = paymentsPage.getContent();
        if (status != null && !status.trim().isEmpty()) {
            try {
                Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
                filteredPayments = filteredPayments.stream()
                    .filter(p -> p.getStatus() == paymentStatus)
                    .toList();
            } catch (IllegalArgumentException e) {
                filteredPayments = List.of();
            }
        }
        if (method != null && !method.trim().isEmpty()) {
            try {
                Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(method.toUpperCase());
                filteredPayments = filteredPayments.stream()
                    .filter(p -> p.getMethod() == paymentMethod)
                    .toList();
            } catch (IllegalArgumentException e) {
                filteredPayments = List.of();
            }
        }

        List<PaymentResponseDTO> paymentDTOs = filteredPayments.stream()
            .map(PaymentResponseDTO::fromEntity)
            .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("payments", paymentDTOs);
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", paymentsPage.getNumber() + 1);
        pagination.put("limit", paymentsPage.getSize());
        pagination.put("total", paymentsPage.getTotalElements());
        pagination.put("pages", paymentsPage.getTotalPages());
        data.put("pagination", pagination);

        return data;
    }

    /**
     * Lấy payment theo ID
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new AppException("Payment not found", 404));
        return PaymentResponseDTO.fromEntity(payment);
    }

    /**
     * Lấy payment theo transactionCode
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByTransactionCode(String transactionCode) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
            .orElseThrow(() -> new AppException("Payment not found", 404));
        return PaymentResponseDTO.fromEntity(payment);
    }

    /**
     * Tạo transaction code: PAY-YYYYMMDD-XXX
     */
    private String generateTransactionCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Đếm số payments đã tạo trong ngày
        long count = paymentRepository.count();
        String sequence = String.format("%03d", (count % 1000) + 1);
        
        return "PAY-" + dateStr + "-" + sequence;
    }
}


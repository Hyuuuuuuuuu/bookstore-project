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
import java.time.LocalDateTime;
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

        // Kiểm tra xem đã có payment nào chưa
        Optional<Payment> existingPayment = paymentRepository.findAll().stream()
            .filter(p -> p.getOrder().getId().equals(orderId))
            .findFirst();
            
        if (existingPayment.isPresent()) {
            return PaymentResponseDTO.fromEntity(existingPayment.get());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount != null ? amount : order.getTotalPrice());
        payment.setMethod(Payment.PaymentMethod.COD);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setDescription(description != null ? description : "Thanh toán khi nhận hàng cho đơn " + order.getOrderCode());
        payment.setTransactionCode(generateTransactionCode(payment.getMethod()));
        
        payment = paymentRepository.save(payment);
        
        return PaymentResponseDTO.fromEntity(payment);
    }

    /**
     * Tạo URL thanh toán Online (VNPay/MoMo - Mock Logic)
     */
    @Transactional
    public Map<String, Object> createOnlinePaymentUrl(Long orderId, String method, Double amount, String ipAddress) {
        Order order = orderRepository.findById(orderId)
            .filter(o -> !o.getIsDeleted())
            .orElseThrow(() -> new AppException("Order not found", 404));

        Payment.PaymentMethod paymentMethod;
        try {
            paymentMethod = Payment.PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("Phương thức thanh toán không hỗ trợ", 400);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount != null ? amount : order.getTotalPrice());
        payment.setMethod(paymentMethod);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setDescription("Thanh toán online qua " + method + " cho đơn " + order.getOrderCode());
        payment.setTransactionCode(generateTransactionCode(payment.getMethod()));
        
        Payment.CustomerInfo info = new Payment.CustomerInfo();
        info.setIpAddress(ipAddress);
        payment.setCustomerInfo(info);

        payment = paymentRepository.save(payment);

        // Mock URL
        String mockPaymentUrl = "";
        if (paymentMethod == Payment.PaymentMethod.VNPAY) {
            mockPaymentUrl = "http://localhost:5000/api/payments/callback/vnpay?status=success&orderId=" + orderId + "&transId=" + payment.getTransactionCode();
        } else if (paymentMethod == Payment.PaymentMethod.MOMO) {
            mockPaymentUrl = "http://localhost:5000/api/payments/callback/momo?status=success&orderId=" + orderId + "&transId=" + payment.getTransactionCode();
        } else {
            mockPaymentUrl = "http://localhost:3000/payment-success?orderId=" + orderId;
        }
        
        payment.setPaymentUrl(mockPaymentUrl);
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("paymentUrl", mockPaymentUrl);
        response.put("transactionCode", payment.getTransactionCode());
        return response;
    }

    /**
     * Xử lý Callback
     */
    @Transactional
    public PaymentResponseDTO handlePaymentCallback(String transactionCode, boolean isSuccess, String gatewayResponse) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
            .orElseThrow(() -> new AppException("Payment transaction not found", 404));

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            return PaymentResponseDTO.fromEntity(payment);
        }

        if (isSuccess) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setGatewayResponse(gatewayResponse);
            
            Order order = payment.getOrder();
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setGatewayResponse(gatewayResponse);
        }

        payment = paymentRepository.save(payment);
        return PaymentResponseDTO.fromEntity(payment);
    }

    /**
     * Cập nhật trạng thái thanh toán thủ công (Admin)
     */
    @Transactional
    public PaymentResponseDTO updatePaymentStatus(Long paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new AppException("Payment not found", 404));

        try {
            Payment.PaymentStatus newStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            payment.setStatus(newStatus);
            
            if (newStatus == Payment.PaymentStatus.COMPLETED) {
                Order order = payment.getOrder();
                order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
                order.setPaidAt(LocalDateTime.now());
                orderRepository.save(order);
            }
            
            payment = paymentRepository.save(payment);
            return PaymentResponseDTO.fromEntity(payment);
        } catch (IllegalArgumentException e) {
            throw new AppException("Invalid payment status", 400);
        }
    }

    /**
     * Lấy danh sách payments (Admin) - CÓ SEARCH
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPayments(Integer page, Integer limit, String search, String status, String method) {
        Pageable pageable = PageRequest.of(
            page != null && page > 0 ? page - 1 : 0,
            limit != null && limit > 0 ? limit : 10,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Payment.PaymentStatus paymentStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equals("all")) {
            try {
                paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Payment.PaymentMethod paymentMethod = null;
        if (method != null && !method.trim().isEmpty() && !method.equals("all")) {
            try {
                paymentMethod = Payment.PaymentMethod.valueOf(method.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // Sử dụng searchPayments từ Repository
        Page<Payment> paymentsPage = paymentRepository.searchPayments(search, paymentStatus, paymentMethod, pageable);

        List<PaymentResponseDTO> paymentDTOs = paymentsPage.getContent().stream()
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

    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new AppException("Payment not found", 404));
        return PaymentResponseDTO.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByTransactionCode(String transactionCode) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
            .orElseThrow(() -> new AppException("Payment not found", 404));
        return PaymentResponseDTO.fromEntity(payment);
    }

    private String generateTransactionCode(Payment.PaymentMethod method) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // Tạo random 4 ký tự từ UUID
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "TXN-" + method.name() + "-" + dateStr + "-" + random;
    }
}
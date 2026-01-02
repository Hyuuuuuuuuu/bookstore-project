package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.PaymentDTO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Lấy danh sách thanh toán (Có hỗ trợ Search & Filter)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllPayments(Integer page, Integer limit, String search, String status) {
        // 1. Phân trang & Sắp xếp (Mới nhất lên đầu)
        Pageable pageable = PageRequest.of(
                page != null && page > 0 ? page - 1 : 0,
                limit != null && limit > 0 ? limit : 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // 2. Xử lý bộ lọc trạng thái (Enum)
        Payment.PaymentStatus paymentStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Nếu status không hợp lệ thì bỏ qua filter này
            }
        }

        // 3. Gọi Repository (Hàm tìm kiếm nâng cao)
        Page<Payment> paymentPage = paymentRepository.findPaymentsWithFilters(search, paymentStatus, pageable);

        // 4. Convert sang DTO
        List<PaymentDTO> paymentDTOS = paymentPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 5. Đóng gói kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("payments", paymentDTOS);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", paymentPage.getNumber() + 1);
        pagination.put("limit", paymentPage.getSize());
        pagination.put("total", paymentPage.getTotalElements());
        pagination.put("totalPages", paymentPage.getTotalPages());
        result.put("pagination", pagination);

        return result;
    }

    /**
     * Helper: Chuyển đổi Entity sang DTO
     */
    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod().name());
        dto.setStatus(payment.getStatus().name());
        dto.setTransactionId(payment.getTransactionId());
        dto.setCreatedAt(payment.getCreatedAt());
        
        // Lấy thông tin đơn hàng và người dùng để hiển thị (tránh lỗi N/A)
        if (payment.getOrder() != null) {
            dto.setOrderId(payment.getOrder().getId());
            dto.setOrderCode(payment.getOrder().getOrderCode());
            
            // Lấy tên khách hàng đưa vào description để frontend hiển thị
            if (payment.getOrder().getUser() != null) {
                dto.setDescription(payment.getOrder().getUser().getName()); 
            } else if (payment.getOrder().getShippingAddress() != null) {
                dto.setDescription(payment.getOrder().getShippingAddress().getName());
            } else {
                dto.setDescription("Khách vãng lai");
            }
        } else {
            dto.setDescription(payment.getDescription());
        }
        
        return dto;
    }

    // --- CÁC HÀM KHÁC (GIỮ NGUYÊN) ---

    @Transactional
    public PaymentDTO createPaymentUrl(Long orderId, String method) {
        // Logic tạo URL thanh toán (VNPAY/Momo...)
        // ... (Giữ nguyên code cũ của bạn ở đây)
        return null; 
    }
    
    @Transactional
    public void updatePaymentStatus(String transactionId, Payment.PaymentStatus status) {
        // ... (Giữ nguyên code cũ của bạn ở đây)
    }
}

package com.hutech.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private Long shippingAddressId;
    private Long shippingProviderId;
    private String paymentMethod; // COD, BANK_TRANSFER, MOMO, ZALOPAY
    private String voucherCode; // Optional
    private String note; // Optional
    private List<OrderItemRequest> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long bookId;
        private Integer quantity;
    }
}


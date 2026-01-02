package com.hutech.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingProviderRequestDTO {
    private String name;
    private String code;
    private Double baseFee;
    private String estimatedTime;
    private Boolean active;
    private String description;
    private ContactInfoDTO contactInfo;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfoDTO {
        private String phone;
        private String email;
        private String website;
    }
}



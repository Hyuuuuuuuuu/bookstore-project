package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.ShippingProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingProviderResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private String name;
    private String code;
    private Double baseFee;
    private String estimatedTime;
    private String status;
    private Boolean active;
    private String description;
    private ContactInfoDTO contactInfo;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfoDTO {
        private String phone;
        private String email;
        private String website;
    }
    
    public static ShippingProviderResponseDTO fromEntity(ShippingProvider provider) {
        if (provider == null) {
            return null;
        }
        
        ShippingProviderResponseDTO dto = new ShippingProviderResponseDTO();
        dto.setId(provider.getId());
        dto.setName(provider.getName());
        dto.setCode(provider.getCode());
        dto.setBaseFee(provider.getBaseFee());
        dto.setEstimatedTime(provider.getEstimatedTime());
        dto.setStatus(provider.getStatus() != null ? provider.getStatus().name() : null);
        dto.setActive(provider.getStatus() == null ? Boolean.TRUE : (provider.getStatus() == com.hutech.bookstore.model.ShippingProvider.Status.ACTIVE));
        dto.setDescription(provider.getDescription());
        dto.setIsDeleted(provider.getIsDeleted());
        dto.setCreatedAt(provider.getCreatedAt());
        dto.setUpdatedAt(provider.getUpdatedAt());
        
        // Convert ContactInfo
        if (provider.getContactInfo() != null) {
            ContactInfoDTO contactInfoDTO = new ContactInfoDTO();
            contactInfoDTO.setPhone(provider.getContactInfo().getPhone());
            contactInfoDTO.setEmail(provider.getContactInfo().getEmail());
            contactInfoDTO.setWebsite(provider.getContactInfo().getWebsite());
            dto.setContactInfo(contactInfoDTO);
        }
        
        return dto;
    }
}


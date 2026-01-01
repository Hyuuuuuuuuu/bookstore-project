package com.hutech.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hutech.bookstore.model.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDTO {
    @JsonProperty("_id")
    private Long id;
    
    private String name;
    private String phone;
    private String address;
    private String city;
    private String district;
    private String ward;
    private Boolean isDefault;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static AddressResponseDTO fromEntity(Address address) {
        if (address == null) {
            return null;
        }
        
        AddressResponseDTO dto = new AddressResponseDTO();
        dto.setId(address.getId());
        dto.setName(address.getName());
        dto.setPhone(address.getPhone());
        dto.setAddress(address.getAddress());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setWard(address.getWard());
        dto.setIsDefault(address.getIsDefault());
        dto.setIsDeleted(address.getIsDeleted());
        dto.setCreatedAt(address.getCreatedAt());
        dto.setUpdatedAt(address.getUpdatedAt());
        
        return dto;
    }
}


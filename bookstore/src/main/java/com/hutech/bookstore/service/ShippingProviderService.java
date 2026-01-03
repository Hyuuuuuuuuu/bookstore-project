package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.ShippingProviderResponseDTO;
import com.hutech.bookstore.dto.ShippingProviderRequestDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.ShippingProvider;
import com.hutech.bookstore.repository.ShippingProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingProviderService {

    private final ShippingProviderRepository shippingProviderRepository;

    /**
     * Lấy danh sách đơn vị vận chuyển đang hoạt động (Public - cho trang Checkout)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActiveShippingProviders() {
        List<ShippingProvider> providers = shippingProviderRepository.findByStatusAndIsDeletedFalse(ShippingProvider.Status.ACTIVE);
        
        List<ShippingProviderResponseDTO> providerDTOs = providers.stream()
            .map(ShippingProviderResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("providers", providerDTOs);
        data.put("total", providerDTOs.size());
        
        return data;
    }

    /**
     * Lấy danh sách đơn vị vận chuyển (Admin - có Filter, Search, Sort)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllShippingProviders(String search, String status, String sortBy, String sortOrder) {
        // Xử lý Sort
        Sort.Direction direction = sortOrder != null && sortOrder.equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        String sortProperty = sortBy != null && !sortBy.isEmpty() ? sortBy : "createdAt";
        Sort sort = Sort.by(direction, sortProperty);

        // Xử lý Status Filter
        ShippingProvider.Status statusEnum = null;
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            try {
                // Map "active"/"inactive" từ frontend sang enum ACTIVE/DISABLED
                if (status.equalsIgnoreCase("active")) {
                    statusEnum = ShippingProvider.Status.ACTIVE;
                } else if (status.equalsIgnoreCase("inactive")) {
                    statusEnum = ShippingProvider.Status.DISABLED;
                } else {
                    statusEnum = ShippingProvider.Status.valueOf(status.toUpperCase());
                }
            } catch (Exception e) {
                // Ignore invalid status
            }
        }

        // Gọi Repository
        List<ShippingProvider> providers = shippingProviderRepository.searchProviders(search, statusEnum, sort);
        
        List<ShippingProviderResponseDTO> providerDTOs = providers.stream()
            .map(ShippingProviderResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("providers", providerDTOs);
        data.put("total", providerDTOs.size());
        
        return data;
    }

    /**
     * Lấy đơn vị vận chuyển theo ID
     */
    @Transactional(readOnly = true)
    public ShippingProviderResponseDTO getShippingProviderById(Long id) {
        ShippingProvider provider = shippingProviderRepository.findById(id)
            .filter(p -> !p.getIsDeleted())
            .orElseThrow(() -> new AppException("Shipping provider not found", 404));
        
        return ShippingProviderResponseDTO.fromEntity(provider);
    }

    /**
     * Lấy đơn vị vận chuyển theo code
     */
    @Transactional(readOnly = true)
    public ShippingProviderResponseDTO getShippingProviderByCode(String code) {
        ShippingProvider provider = shippingProviderRepository.findByCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new AppException("Shipping provider not found", 404));
        
        return ShippingProviderResponseDTO.fromEntity(provider);
    }

    /**
     * Tạo mới đơn vị vận chuyển
     */
    @Transactional
    public ShippingProviderResponseDTO createShippingProvider(ShippingProviderRequestDTO dto) {
        if (dto == null) throw new AppException("Request body is required", 400);
        if (dto.getName() == null || dto.getName().trim().isEmpty()) throw new AppException("Name is required", 400);
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) throw new AppException("Code is required", 400);

        String code = dto.getCode().trim().toUpperCase();
        if (shippingProviderRepository.findByCodeAndIsDeletedFalse(code).isPresent()) {
            throw new AppException("Shipping provider code already exists", 409);
        }

        ShippingProvider provider = new ShippingProvider();
        provider.setName(dto.getName().trim());
        provider.setCode(code);
        provider.setBaseFee(dto.getBaseFee() != null ? dto.getBaseFee() : 0.0);
        provider.setEstimatedTime(dto.getEstimatedTime() != null ? dto.getEstimatedTime().trim() : "");
        
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            provider.setStatus(ShippingProvider.Status.valueOf(dto.getStatus().trim().toUpperCase()));
        } else {
            provider.setStatus(ShippingProvider.Status.ACTIVE);
        }
        
        provider.setDescription(dto.getDescription());

        if (dto.getContactInfo() != null) {
            ShippingProvider.ContactInfo ci = new ShippingProvider.ContactInfo();
            ci.setPhone(dto.getContactInfo().getPhone());
            ci.setEmail(dto.getContactInfo().getEmail());
            ci.setWebsite(dto.getContactInfo().getWebsite());
            provider.setContactInfo(ci);
        }

        provider = shippingProviderRepository.save(provider);
        return ShippingProviderResponseDTO.fromEntity(provider);
    }

    /**
     * Cập nhật đơn vị vận chuyển
     */
    @Transactional
    public ShippingProviderResponseDTO updateShippingProvider(Long id, ShippingProviderRequestDTO dto) {
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new AppException("Shipping provider not found", 404));

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            provider.setName(dto.getName().trim());
        }

        if (dto.getCode() != null && !dto.getCode().trim().isEmpty()) {
            String newCode = dto.getCode().trim().toUpperCase();
            if (!newCode.equals(provider.getCode()) && shippingProviderRepository.findByCodeAndIsDeletedFalse(newCode).isPresent()) {
                throw new AppException("Shipping provider code already exists", 409);
            }
            provider.setCode(newCode);
        }

        if (dto.getBaseFee() != null) provider.setBaseFee(dto.getBaseFee());
        if (dto.getEstimatedTime() != null) provider.setEstimatedTime(dto.getEstimatedTime());

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            try {
                provider.setStatus(ShippingProvider.Status.valueOf(dto.getStatus().trim().toUpperCase()));
            } catch (Exception e) {
                // Ignore invalid status enum
            }
        }

        if (dto.getDescription() != null) provider.setDescription(dto.getDescription());

        if (dto.getContactInfo() != null) {
            ShippingProvider.ContactInfo ci = provider.getContactInfo() != null ? provider.getContactInfo() : new ShippingProvider.ContactInfo();
            ci.setPhone(dto.getContactInfo().getPhone());
            ci.setEmail(dto.getContactInfo().getEmail());
            ci.setWebsite(dto.getContactInfo().getWebsite());
            provider.setContactInfo(ci);
        }

        provider = shippingProviderRepository.save(provider);
        return ShippingProviderResponseDTO.fromEntity(provider);
    }

    /**
     * Xóa đơn vị vận chuyển (Soft delete)
     */
    @Transactional
    public void deleteShippingProvider(Long id) {
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new AppException("Shipping provider not found", 404));

        provider.setIsDeleted(true);
        provider.setStatus(ShippingProvider.Status.DISABLED);
        shippingProviderRepository.save(provider);
    }
}
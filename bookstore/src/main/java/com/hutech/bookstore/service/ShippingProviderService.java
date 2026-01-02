package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.ShippingProviderResponseDTO;
import com.hutech.bookstore.dto.ShippingProviderRequestDTO;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.ShippingProvider;
import com.hutech.bookstore.repository.ShippingProviderRepository;
import lombok.RequiredArgsConstructor;
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
     * Lấy tất cả đơn vị vận chuyển (chỉ active và chưa xóa)
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
     * Lấy tất cả đơn vị vận chuyển (bao gồm cả inactive và đã xóa - Admin only)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllShippingProviders() {
        List<ShippingProvider> providers = shippingProviderRepository.findAll();
        
        // Filter out deleted providers
        List<ShippingProvider> activeProviders = providers.stream()
            .filter(p -> !p.getIsDeleted())
            .collect(Collectors.toList());
        
        List<ShippingProviderResponseDTO> providerDTOs = activeProviders.stream()
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
        // Basic validation
        if (dto == null) {
            throw new AppException("Request body is required", 400);
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new AppException("Name is required", 400);
        }
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new AppException("Code is required", 400);
        }
        String code = dto.getCode().trim().toUpperCase();
        // Ensure unique code
        if (shippingProviderRepository.findByCodeAndIsDeletedFalse(code).isPresent()) {
            throw new AppException("Shipping provider code already exists", 409);
        }

        ShippingProvider provider = new ShippingProvider();
        provider.setName(dto.getName().trim());
        provider.setCode(code);
        provider.setBaseFee(dto.getBaseFee() != null ? dto.getBaseFee() : 0.0);
        provider.setEstimatedTime(dto.getEstimatedTime() != null ? dto.getEstimatedTime().trim() : "");
        // set status: prefer explicit status string if provided, otherwise fall back to active boolean, default ACTIVE
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            provider.setStatus(ShippingProvider.Status.valueOf(dto.getStatus().trim().toUpperCase()));
        } else if (dto.getActive() != null) {
            provider.setStatus(dto.getActive() ? ShippingProvider.Status.ACTIVE : ShippingProvider.Status.DISABLED);
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
     * Cập nhật đơn vị vận chuyển (Admin)
     */
    @Transactional
    public ShippingProviderResponseDTO updateShippingProvider(Long id, ShippingProviderRequestDTO dto) {
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new AppException("Shipping provider not found", 404));

        if (dto == null) {
            throw new AppException("Request body is required", 400);
        }

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

        if (dto.getBaseFee() != null) {
            provider.setBaseFee(dto.getBaseFee());
        }

        if (dto.getEstimatedTime() != null) {
            provider.setEstimatedTime(dto.getEstimatedTime());
        }

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            provider.setStatus(ShippingProvider.Status.valueOf(dto.getStatus().trim().toUpperCase()));
        } else if (dto.getActive() != null) {
            provider.setStatus(dto.getActive() ? ShippingProvider.Status.ACTIVE : ShippingProvider.Status.DISABLED);
        }

        if (dto.getDescription() != null) {
            provider.setDescription(dto.getDescription());
        }

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
     * Soft-delete đơn vị vận chuyển (Admin)
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


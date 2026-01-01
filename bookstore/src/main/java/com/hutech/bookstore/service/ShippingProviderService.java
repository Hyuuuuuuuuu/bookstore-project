package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.ShippingProviderResponseDTO;
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
        List<ShippingProvider> providers = shippingProviderRepository.findByActiveTrueAndIsDeletedFalse();
        
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
}


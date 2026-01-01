package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.AddressResponseDTO;
import com.hutech.bookstore.dto.CreateAddressRequest;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.Address;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    /**
     * Lấy danh sách địa chỉ của user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAddresses(User user) {
        List<Address> addresses = addressRepository.findByUserAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(user);
        
        List<AddressResponseDTO> addressDTOs = addresses.stream()
            .map(AddressResponseDTO::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> data = new HashMap<>();
        data.put("addresses", addressDTOs);
        data.put("total", addressDTOs.size());
        
        return data;
    }

    /**
     * Lấy địa chỉ mặc định của user
     */
    @Transactional(readOnly = true)
    public AddressResponseDTO getDefaultAddress(User user) {
        Address address = addressRepository.findByUserAndIsDefaultTrueAndIsDeletedFalse(user)
            .orElse(null);
        
        return AddressResponseDTO.fromEntity(address);
    }

    /**
     * Tạo địa chỉ mới
     */
    @Transactional
    public AddressResponseDTO createAddress(User user, CreateAddressRequest request) {
        // Nếu đặt làm mặc định, bỏ mặc định của các địa chỉ khác
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            List<Address> defaultAddresses = addressRepository.findByUserAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(user)
                .stream()
                .filter(Address::getIsDefault)
                .collect(Collectors.toList());
            
            for (Address addr : defaultAddresses) {
                addr.setIsDefault(false);
            }
            addressRepository.saveAll(defaultAddresses);
        }
        
        // Tạo địa chỉ mới
        Address address = new Address();
        address.setUser(user);
        address.setName(request.getName().trim());
        address.setPhone(request.getPhone().trim());
        address.setAddress(request.getAddress().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard().trim());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        address.setIsDeleted(false);
        
        address = addressRepository.save(address);
        
        return AddressResponseDTO.fromEntity(address);
    }

    /**
     * Cập nhật địa chỉ
     */
    @Transactional
    public AddressResponseDTO updateAddress(User user, Long addressId, CreateAddressRequest request) {
        Address address = addressRepository.findById(addressId)
            .filter(addr -> addr.getUser().getId().equals(user.getId()) && !addr.getIsDeleted())
            .orElseThrow(() -> new AppException("Address not found or access denied", 404));
        
        // Nếu đặt làm mặc định, bỏ mặc định của các địa chỉ khác
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            List<Address> defaultAddresses = addressRepository.findByUserAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(user)
                .stream()
                .filter(addr -> !addr.getId().equals(addressId) && addr.getIsDefault())
                .collect(Collectors.toList());
            
            for (Address addr : defaultAddresses) {
                addr.setIsDefault(false);
            }
            addressRepository.saveAll(defaultAddresses);
        }
        
        // Cập nhật thông tin
        address.setName(request.getName().trim());
        address.setPhone(request.getPhone().trim());
        address.setAddress(request.getAddress().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard().trim());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        
        address = addressRepository.save(address);
        
        return AddressResponseDTO.fromEntity(address);
    }

    /**
     * Xóa địa chỉ (soft delete)
     */
    @Transactional
    public void deleteAddress(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
            .filter(addr -> addr.getUser().getId().equals(user.getId()) && !addr.getIsDeleted())
            .orElseThrow(() -> new AppException("Address not found or access denied", 404));
        
        address.setIsDeleted(true);
        addressRepository.save(address);
    }

    /**
     * Đặt địa chỉ làm mặc định
     */
    @Transactional
    public AddressResponseDTO setDefaultAddress(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
            .filter(addr -> addr.getUser().getId().equals(user.getId()) && !addr.getIsDeleted())
            .orElseThrow(() -> new AppException("Address not found or access denied", 404));
        
        // Bỏ mặc định của các địa chỉ khác
        List<Address> defaultAddresses = addressRepository.findByUserAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(user)
            .stream()
            .filter(addr -> !addr.getId().equals(addressId) && addr.getIsDefault())
            .collect(Collectors.toList());
        
        for (Address addr : defaultAddresses) {
            addr.setIsDefault(false);
        }
        addressRepository.saveAll(defaultAddresses);
        
        // Đặt địa chỉ này làm mặc định
        address.setIsDefault(true);
        address = addressRepository.save(address);
        
        return AddressResponseDTO.fromEntity(address);
    }
}


package com.hutech.bookstore.service;

import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.UserRepository;
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
import com.hutech.bookstore.dto.CreateUserRequest;
import com.hutech.bookstore.model.Role;
import com.hutech.bookstore.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; 
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> getUsers(Integer page, Integer limit, String search, String role) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(
                (page != null && page > 0) ? page - 1 : 0,
                (limit != null && limit > 0) ? limit : 10,
                sort
        );

        // Xử lý filter role
        String roleFilter = (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("all")) 
                            ? role.trim() : null;

        Page<User> usersPage = userRepository.findUsersWithFilters(search, roleFilter, pageable);

        // Convert sang Map/DTO để trả về (ẩn password)
        List<Map<String, Object>> userDTOs = usersPage.getContent().stream().map(u -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", u.getId());
            dto.put("name", u.getName());
            dto.put("email", u.getEmail());
            dto.put("phone", u.getPhone());
            dto.put("role", u.getRole().getName());
            dto.put("status", u.getStatus());
            dto.put("isActive", u.getIsActive());
            dto.put("createdAt", u.getCreatedAt());
            // Giả lập số liệu thống kê (cần query thật nếu muốn chính xác)
            dto.put("totalOrders", 0); 
            dto.put("totalSpent", 0);
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("users", userDTOs);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", usersPage.getNumber() + 1);
        pagination.put("limit", usersPage.getSize());
        pagination.put("total", usersPage.getTotalElements());
        pagination.put("pages", usersPage.getTotalPages());
        data.put("pagination", pagination);

        return data;
    }
    
    // Khóa/Mở khóa tài khoản
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));
        
        // Đảo ngược trạng thái
        if (user.getStatus() == User.UserStatus.ACTIVE) {
            user.setStatus(User.UserStatus.INACTIVE);
            user.setIsActive(false);
        } else {
            user.setStatus(User.UserStatus.ACTIVE);
            user.setIsActive(true);
        }
        userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", 404));
        user.setIsDeleted(true);
        userRepository.save(user);
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã tồn tại trong hệ thống", 400);
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new AppException("Vai trò không hợp lệ", 400));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setIsEmailVerified(true); // Admin tạo thì auto verify
        user.setStatus(User.UserStatus.ACTIVE);
        user.setIsActive(true);

        return userRepository.save(user);
    }
}
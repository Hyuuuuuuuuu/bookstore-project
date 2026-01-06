package com.hutech.bookstore.controller;

import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.UserRepository;
import com.hutech.bookstore.repository.OrderRepository;
import com.hutech.bookstore.model.Order;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role
    ) {
        // Require authenticated user (SecurityConfig already enforces authentication)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "Unauthorized"));
        }

        User currentUser = (User) authentication.getPrincipal();
        // Only allow admin role to list all users
        String currentRole = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (currentRole == null || !currentRole.equalsIgnoreCase("admin")) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "Access denied. Admins only."));
        }

        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, Math.max(limit, 1), Sort.by("createdAt").descending());

        Page<User> usersPage = userRepository.findAll(pageable);

        List<Map<String, Object>> users = usersPage.getContent().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("avatar", u.getAvatar());
            m.put("role", u.getRole() != null ? u.getRole().getName() : null);
            // Normalize status to two values: active or locked
            String normalizedStatus = (u.getStatus() != null && "ACTIVE".equalsIgnoreCase(u.getStatus().name())) ? "active" : "locked";
            m.put("status", normalizedStatus);
            m.put("isEmailVerified", u.getIsEmailVerified());
            m.put("isActive", u.getIsActive());
            m.put("createdAt", u.getCreatedAt());

            // Compute order statistics for user
            Page<Order> userOrders = orderRepository.findByUserAndIsDeletedFalse(u, Pageable.unpaged());
            long totalOrders = userOrders.getTotalElements();
            double totalSpent = userOrders.getContent().stream()
                    .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0)
                    .sum();

            m.put("totalOrders", totalOrders);
            m.put("totalSpent", totalSpent);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", usersPage.getNumber() + 1);
        pagination.put("limit", usersPage.getSize());
        pagination.put("total", usersPage.getTotalElements());
        pagination.put("pages", usersPage.getTotalPages());
        data.put("pagination", pagination);

        return ResponseEntity.ok(ApiResponse.success(data, "Users retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Long id) {
        // Require authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "Unauthorized"));
        }

        User currentUser = (User) authentication.getPrincipal();
        boolean isAdmin = currentUser.getRole() != null && "admin".equalsIgnoreCase(currentUser.getRole().getName());
        // Allow admins or the user themself
        if (!isAdmin && !currentUser.getId().equals(id)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "Access denied."));
        }

        return userRepository.findById(id).map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("avatar", u.getAvatar());
            m.put("role", u.getRole() != null ? u.getRole().getName() : null);
            String normalizedStatus = (u.getStatus() != null && "ACTIVE".equalsIgnoreCase(u.getStatus().name())) ? "active" : "locked";
            m.put("status", normalizedStatus);
            m.put("isEmailVerified", u.getIsEmailVerified());
            m.put("isActive", u.getIsActive());
            m.put("createdAt", u.getCreatedAt());

            // Compute order statistics for user
            Page<Order> userOrders = orderRepository.findByUserAndIsDeletedFalse(u, Pageable.unpaged());
            long totalOrders = userOrders.getTotalElements();
            double totalSpent = userOrders.getContent().stream()
                    .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0.0)
                    .sum();

            m.put("totalOrders", totalOrders);
            m.put("totalSpent", totalSpent);

            return ResponseEntity.ok(ApiResponse.success(m, "User retrieved successfully"));
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error(404, "User not found")));
    }
}



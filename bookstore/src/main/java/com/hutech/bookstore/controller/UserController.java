package com.hutech.bookstore.controller;

import com.hutech.bookstore.service.UserService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.hutech.bookstore.dto.CreateUserRequest;
import com.hutech.bookstore.model.User;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        
        Map<String, Object> data = userService.getUsers(page, limit, search, role);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Users retrieved successfully"));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "User status updated"));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse<>(200, null, "User deleted successfully"));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody CreateUserRequest request) {
        User newUser = userService.createUser(request);
        return ResponseEntity.status(201)
                .body(new ApiResponse<>(201, newUser, "User created successfully"));
    }
}
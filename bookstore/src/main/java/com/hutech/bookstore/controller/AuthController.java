package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.*;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.UserRepository;
import com.hutech.bookstore.security.JwtAuthenticationFilter;
import com.hutech.bookstore.service.AuthService;
import com.hutech.bookstore.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "User registered successfully. Please check your email for verification code.");
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());
        userData.put("roleId", user.getRole().getId());
        userData.put("isEmailVerified", user.getIsEmailVerified());
        data.put("user", userData);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, data, "Registration successful. Email verification required."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody AuthRequest request) {
        String token = authService.login(request);
        User user = authService.getCurrentUser(request.getEmail());

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());
        userData.put("role", user.getRole().getName());
        userData.put("avatar", user.getAvatar());
        userData.put("phone", user.getPhone());
        userData.put("address", user.getAddress());
        userData.put("isEmailVerified", user.getIsEmailVerified());
        data.put("user", userData);
        data.put("token", token);

        return ResponseEntity.ok(new ApiResponse<>(200, data, "Login successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());
        userData.put("role", user.getRole().getName());
        userData.put("avatar", user.getAvatar());
        userData.put("phone", user.getPhone());
        userData.put("address", user.getAddress());
        userData.put("isEmailVerified", user.getIsEmailVerified());
        userData.put("status", user.getStatus());
        userData.put("isActive", user.getIsActive());

        return ResponseEntity
                .ok(new ApiResponse<>(200, Map.of("user", userData), "User profile retrieved successfully"));
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request.getEmail(), request.getName());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Verification code sent to your email");

        return ResponseEntity.ok(new ApiResponse<>(200, data, "Verification code sent successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmailCode(request.getEmail(), request.getCode());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Email verified successfully");

        return ResponseEntity.ok(new ApiResponse<>(200, data, "Email verification successful"));
    }

    @PostMapping("/register-with-verification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerWithVerification(
            @Valid @RequestBody RegisterWithVerificationRequest request) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(request.getName());
        registerRequest.setEmail(request.getEmail());
        registerRequest.setPassword(request.getPassword());
        registerRequest.setPhone(request.getPhone());
        registerRequest.setAddress(request.getAddress());

        User user = authService.registerWithVerification(registerRequest, request.getVerificationCode());
        String token = authService.login(new AuthRequest(request.getEmail(), request.getPassword()));

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());
        userData.put("roleId", user.getRole().getId());
        userData.put("isEmailVerified", user.getIsEmailVerified());
        data.put("user", userData);
        data.put("token", token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, data, "User registered with verification successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "OTP code sent to your email for password reset");

        return ResponseEntity.ok(new ApiResponse<>(200, data, "OTP sent successfully"));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyResetOTP(
            @Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyResetOTP(request.getEmail(), request.getCode());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "OTP verified successfully. You can now reset your password.");

        return ResponseEntity.ok(new ApiResponse<>(200, data, "OTP verification successful"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getCode(), request.getPassword());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Password reset successfully");

        return ResponseEntity.ok(new ApiResponse<>(200, data, "Password reset successful"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        authService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Password changed successfully");

        return ResponseEntity.ok(new ApiResponse<>(200, data, "Password changed successfully"));
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, null, "Please select a file to upload"));
            }

            // Check file type (only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, null, "Only image files are allowed"));
            }

            // Check file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, null, "File size must be less than 5MB"));
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + extension;

            // Create uploads directory if it doesn't exist - use classpath resource
            try {
                // Get the resource directory path
                String resourcePath = getClass().getClassLoader().getResource("").getPath();
                String uploadPath = resourcePath + "static/uploads/avatars";
                java.io.File uploadsDir = new java.io.File(uploadPath);
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdirs();
                }

                // Save file to uploads directory
                String filePath = uploadPath + "/" + filename;
                file.transferTo(new java.io.File(filePath));
                System.out.println("File saved to: " + filePath);
                System.out.println("Upload directory: " + uploadPath);
            } catch (Exception e) {
                System.err.println("Failed to save file: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to save avatar file", e);
            }

            // Create avatar URL
            String avatarUrl = "/uploads/avatars/" + filename;

            // Update user avatar
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            // Return updated user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("name", user.getName());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole().getName());
            userData.put("avatar", user.getAvatar());
            userData.put("phone", user.getPhone());
            userData.put("address", user.getAddress());
            userData.put("isEmailVerified", user.getIsEmailVerified());
            userData.put("status", user.getStatus());
            userData.put("isActive", user.getIsActive());

            Map<String, Object> data = new HashMap<>();
            data.put("user", userData);
            data.put("avatarUrl", avatarUrl);
            data.put("message", "Avatar uploaded successfully");

            return ResponseEntity.ok(new ApiResponse<>(200, data, "Avatar uploaded successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, null, "Failed to upload avatar: " + e.getMessage()));
        }
    }
}

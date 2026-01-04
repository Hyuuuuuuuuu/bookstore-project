package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.*;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.repository.UserRepository;
import com.hutech.bookstore.security.JwtAuthenticationFilter;
import com.hutech.bookstore.service.AuthService;
import com.hutech.bookstore.service.FileUploadService;
import com.hutech.bookstore.util.ApiResponse;
import com.hutech.bookstore.exception.AppException;
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
    private final FileUploadService fileUploadService;

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
        // Normalize status to two values: active or locked
        String normalizedStatus = (user.getStatus() != null && "ACTIVE".equalsIgnoreCase(user.getStatus().name())) ? "active" : "locked";
        userData.put("status", normalizedStatus);
        userData.put("isActive", user.getIsActive());

        return ResponseEntity
                .ok(new ApiResponse<>(200, Map.of("user", userData), "User profile retrieved successfully"));
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        try {
            authService.sendVerificationCode(request.getEmail(), request.getName());

            Map<String, Object> data = new HashMap<>();
            data.put("message", "Verification code sent to your email");

            return ResponseEntity.ok(new ApiResponse<>(200, data, "Verification code sent successfully"));
        } catch (AppException ex) {
            // If the email is already verified, return 200 with a clear message instead of 400
            if ("Email already verified".equalsIgnoreCase(ex.getMessage())) {
                Map<String, Object> data = new HashMap<>();
                data.put("message", "Email already verified");
                return ResponseEntity.ok(new ApiResponse<>(200, data, "Email already verified"));
            }
            // Re-throw other application exceptions to be handled globally
            throw ex;
        }
    }

    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEmail(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        boolean exists = userRepository.findByEmailAndIsDeletedFalse(email).isPresent();
        Map<String, Object> data = new HashMap<>();
        data.put("exists", exists);
        return ResponseEntity.ok(new ApiResponse<>(200, data, "Email existence checked"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        // Validate code but do not consume it here (consumed at final registration)
        authService.validateEmailCode(request.getEmail(), request.getCode());

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Verification code is valid");

        return ResponseEntity.ok(new ApiResponse<>(200, data, "Verification code valid"));
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
            // Basic validation: non-empty
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, null, "Please select a file to upload"));
            }

            // Delegate file saving to FileUploadService which handles type/size validation and directory setup
            String filePath = fileUploadService.uploadAvatar(file); // returns e.g. "avatars/uuid.jpg"
            String avatarUrl = fileUploadService.getFileUrl(filePath); // returns e.g. "/uploads/avatars/uuid.jpg"

            // Update user avatar
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            // Prepare response
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

        } catch (IllegalArgumentException e) {
            // FileUploadService may throw IllegalArgumentException for validation errors
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, null, "Failed to upload avatar: " + e.getMessage()));
        }
    }
}

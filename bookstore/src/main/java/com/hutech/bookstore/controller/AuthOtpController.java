package com.hutech.bookstore.controller;

import com.hutech.bookstore.dto.SendOtpRequest;
import com.hutech.bookstore.dto.VerifyOtpRequest;
import com.hutech.bookstore.service.EmailService;
import com.hutech.bookstore.service.OtpService;
import com.hutech.bookstore.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthOtpController {

    private final OtpService otpService;
    private final EmailService emailService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@RequestBody SendOtpRequest request) {
        try {
            String email = request.getEmail();
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Email is required"));
            }
            String code = otpService.generateOtp(email);
            String subject = "Mã OTP đăng ký BookStore";
            String body = String.format("Mã OTP của bạn là: %s\nMã có hiệu lực trong 5 phút.\nNếu bạn không yêu cầu mã này, hãy bỏ qua email.", code);
            emailService.sendSimpleEmail(email, subject, body);
            return ResponseEntity.ok(ApiResponse.success(Map.of("sent", true), "OTP sent"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error(500, "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            String email = request.getEmail();
            String code = request.getCode();
            if (email == null || code == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Email and code required"));
            }
            boolean ok = otpService.verifyOtp(email, code);
            if (!ok) {
                return ResponseEntity.status(400).body(ApiResponse.error(400, "Invalid or expired OTP"));
            }
            return ResponseEntity.ok(ApiResponse.success(Map.of("verified", true), "OTP verified"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error(500, "Verification failed: " + e.getMessage()));
        }
    }

}



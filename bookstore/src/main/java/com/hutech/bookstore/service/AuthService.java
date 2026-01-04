package com.hutech.bookstore.service;

import com.hutech.bookstore.dto.AuthRequest;
import com.hutech.bookstore.dto.RegisterRequest;
import com.hutech.bookstore.model.EmailVerification;
import com.hutech.bookstore.model.Role;
import com.hutech.bookstore.model.User;
import com.hutech.bookstore.exception.AppException;
import com.hutech.bookstore.repository.EmailVerificationRepository;
import com.hutech.bookstore.repository.RoleRepository;
import com.hutech.bookstore.repository.UserRepository;
import com.hutech.bookstore.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("User already exists", 400);
        }

        Role userRole = roleRepository.findByName("user")
            .orElseGet(() -> roleRepository.save(new Role(null, "user", "Default user role", false, null, null)));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(userRole);
        user.setIsEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Generate verification code
        String code = generateVerificationCode();
        EmailVerification verification = new EmailVerification();
        verification.setEmail(savedUser.getEmail());
        verification.setCode(code);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailVerificationRepository.save(verification);

        // TODO: Send email with verification code

        return savedUser;
    }

    public String login(AuthRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
            .orElseThrow(() -> new AppException("Invalid credentials", 401));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException("Invalid credentials", 401);
        }

        return jwtUtil.generateToken(user.getId(), user.getEmail());
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> new AppException("User not found", 404));
    }

    @Transactional
    public void sendVerificationCode(String email, String name) {
        // Check if user already exists
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && existingUser.getIsEmailVerified()) {
            throw new AppException("Email already verified", 400);
        }

        // Delete old verification codes
        emailVerificationRepository.deleteByEmail(email);

        // Generate new code
        String code = generateVerificationCode();
        EmailVerification verification = new EmailVerification();
        verification.setEmail(email.toLowerCase());
        verification.setCode(code);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        emailVerificationRepository.save(verification);

        // Send email with verification code
        emailService.sendEmailVerificationCode(email, code, name);
    }

    @Transactional
    public void verifyEmailCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationRepository
            .findByEmailAndCodeAndIsUsedFalseAndExpiresAtAfter(email.toLowerCase(), code, now)
            .orElseThrow(() -> new AppException("Invalid or expired verification code", 400));

        if (verification.getAttempts() >= 3) {
            throw new AppException("Too many attempts. Please request a new code", 400);
        }

        verification.setIsUsed(true);
        emailVerificationRepository.save(verification);

        // Update user email verification status if user exists (do not fail if user not yet created)
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setIsEmailVerified(true);
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public void validateEmailCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationRepository
            .findByEmailAndCodeAndIsUsedFalseAndExpiresAtAfter(email.toLowerCase(), code, now)
            .orElseThrow(() -> new AppException("Invalid or expired verification code", 400));

        if (verification.getAttempts() >= 3) {
            throw new AppException("Too many attempts. Please request a new code", 400);
        }
        // validation only: do not mark as used here
    }

    @Transactional
    public User registerWithVerification(RegisterRequest request, String verificationCode) {
        // Verify code first (this will mark code as used and set user verified if exists)
        verifyEmailCode(request.getEmail(), verificationCode);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("User already exists", 400);
        }

        Role userRole = roleRepository.findByName("user")
            .orElseGet(() -> roleRepository.save(new Role(null, "user", "Default user role", false, null, null)));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(userRole);
        user.setIsEmailVerified(true);

        User savedUser = userRepository.save(user);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());
        } catch (Exception e) {
            // Log error but don't fail registration if email fails
            System.err.println("Failed to send welcome email to " + savedUser.getEmail() + ": " + e.getMessage());
        }

        return savedUser;
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> new AppException("User not found", 404));

        // Delete old verification codes
        emailVerificationRepository.deleteByEmail(email);

        // Generate OTP code
        String code = generateVerificationCode();
        EmailVerification verification = new EmailVerification();
        verification.setEmail(email.toLowerCase());
        verification.setCode(code);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailVerificationRepository.save(verification);

        // Send email with OTP code
        emailService.sendPasswordResetOTP(email, code, user.getName());
    }

    @Transactional
    public void verifyResetOTP(String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationRepository
            .findByEmailAndCodeAndIsUsedFalseAndExpiresAtAfter(email.toLowerCase(), code, now)
            .orElseThrow(() -> new AppException("Invalid or expired verification code", 400));

        if (verification.getAttempts() >= 3) {
            throw new AppException("Too many attempts. Please request a new code", 400);
        }

        // Don't mark as used yet, will be used in resetPassword
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationRepository
            .findByEmailAndCodeAndIsUsedFalseAndExpiresAtAfter(email.toLowerCase(), code, now)
            .orElseThrow(() -> new AppException("Invalid or expired verification code", 400));

        if (verification.getAttempts() >= 3) {
            throw new AppException("Too many attempts. Please request a new code", 400);
        }

        User user = userRepository.findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> new AppException("User not found", 404));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        verification.setIsUsed(true);
        emailVerificationRepository.save(verification);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException("User not found", 404));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AppException("Current password is incorrect", 400);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }
}


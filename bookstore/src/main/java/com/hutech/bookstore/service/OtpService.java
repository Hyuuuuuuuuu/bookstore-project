package com.hutech.bookstore.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private final SecureRandom secureRandom = new SecureRandom();

    private static class OtpEntry {
        final String code;
        final long expiresAt;

        OtpEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    // Keyed by email
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public String generateOtp(String email) {
        Objects.requireNonNull(email, "email required");
        String code = generateNumericOtp();
        long expiresAt = Instant.now().toEpochMilli() + DEFAULT_TTL_MS;
        otpStore.put(email.toLowerCase().trim(), new OtpEntry(code, expiresAt));
        return code;
    }

    public boolean verifyOtp(String email, String code) {
        if (email == null || code == null) return false;
        String key = email.toLowerCase().trim();
        OtpEntry entry = otpStore.get(key);
        if (entry == null) return false;
        long now = Instant.now().toEpochMilli();
        if (now > entry.expiresAt) {
            otpStore.remove(key);
            return false;
        }
        boolean ok = entry.code.equals(code.trim());
        if (ok) {
            otpStore.remove(key);
        }
        return ok;
    }

    private String generateNumericOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int num = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", num);
    }
}



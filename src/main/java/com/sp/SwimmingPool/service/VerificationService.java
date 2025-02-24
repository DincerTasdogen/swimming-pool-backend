package com.sp.SwimmingPool.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {
    @Data
    private static class VerificationData {
        private final String code;
        private final LocalDateTime expiryTime;
        private final Map<String, Object> tempUserData;
    }

    // Using ConcurrentHashMap for thread safety
    private final Map<String, VerificationData> verificationCodes = new ConcurrentHashMap<>();
    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 5;

    public String generateAndStoreCode(String email, Map<String, Object> userData) {
        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        // Store code with expiry time and user data
        verificationCodes.put(email, new VerificationData(
                code,
                LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES),
                userData
        ));

        return code;
    }

    public boolean verifyCode(String email, String code) {
        VerificationData data = verificationCodes.get(email);
        if (data == null) {
            return false;
        }

        // Check if code is expired
        if (LocalDateTime.now().isAfter(data.getExpiryTime())) {
            verificationCodes.remove(email);
            return false;
        }

        // Check if code matches
        return data.getCode().equals(code);
    }

    public Map<String, Object> getTempUserData(String email) {
        VerificationData data = verificationCodes.get(email);
        return data != null ? data.getTempUserData() : null;
    }

    public void removeVerificationData(String email) {
        verificationCodes.remove(email);
    }

    // Cleanup expired codes periodically (you might want to use @Scheduled)
    public void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodes.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().getExpiryTime()));
    }
}
package com.sp.SwimmingPool.service;

import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;
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
        private LocalDateTime expiryTime; // Changed to non-final to allow extension
        private Map<String, Object> tempUserData;
        private final boolean isOAuthUser; // Added to identify OAuth users

        // Constructor for regular users (with verification code)
        public VerificationData(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.tempUserData = new ConcurrentHashMap<>(); // Initialize as empty map instead of null
            this.isOAuthUser = false;
        }

        // Constructor for OAuth users (no verification code needed)
        public VerificationData(LocalDateTime expiryTime, boolean isOAuthUser) {
            this.code = null; // OAuth users don't need a verification code
            this.expiryTime = expiryTime;
            this.tempUserData = new ConcurrentHashMap<>();
            this.isOAuthUser = isOAuthUser;
        }

        // Method to update tempUserData
        public void updateTempUserData(Map<String, Object> newData) {
            if (newData != null) {
                this.tempUserData.putAll(newData);
            }
        }

        // Method to check if data is expired
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(this.expiryTime);
        }

        // Method to extend expiry time
        public void extendExpiry(int additionalMinutes) {
            this.expiryTime = LocalDateTime.now().plusMinutes(additionalMinutes);
        }
    }

    // Using ConcurrentHashMap for thread safety
    private final Map<String, VerificationData> verificationData = new ConcurrentHashMap<>();
    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 15; // Extended to 15 minutes

    /**
     * Generates and stores a verification code for the given email
     */
    public String generateAndStoreCode(String email, Map<String, Object> userData) {
        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        // Create VerificationData object
        VerificationData data = new VerificationData(
                code,
                LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES)
        );

        // Set the userData
        if (userData != null) {
            data.setTempUserData(userData);
        }

        // Store the verification data
        verificationData.put(email, data);

        return code;
    }

    /**
     * Stores temporary data for OAuth users (no verification code needed)
     */
    public void storeTempUserData(String email, Map<String, Object> userData) {
        // Create OAuth user data (no verification code)
        VerificationData data = new VerificationData(
                LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES),
                true
        );

        // Set the user data
        if (userData != null) {
            data.setTempUserData(userData);
        }

        // Store the data
        verificationData.put(email, data);
    }

    /**
     * Verifies a code for the given email
     */
    public boolean verifyCode(String email, String code) {
        VerificationData data = verificationData.get(email);
        if (data == null) {
            return false;
        }

        // OAuth users don't need verification
        if (data.isOAuthUser()) {
            return true;
        }

        // Check if code is expired
        if (data.isExpired()) {
            verificationData.remove(email);
            return false;
        }

        // Check if code matches
        return data.getCode() != null && data.getCode().equals(code);
    }

    /**
     * Gets the temporary user data for the given email
     */
    public Map<String, Object> getTempUserData(String email) {
        VerificationData data = verificationData.get(email);
        if (data == null || data.isExpired()) {
            return null;
        }
        return data.getTempUserData();
    }

    /**
     * Updates the temporary user data for the specified email
     *
     * @param email Email address
     * @param newData New data to merge with existing user data
     * @return true if updated successfully, false if no verification data exists
     */
    public boolean updateTempUserData(String email, Map<String, Object> newData) {
        VerificationData data = verificationData.get(email);
        if (data == null || data.isExpired()) {
            return false;
        }

        data.updateTempUserData(newData);
        return true;
    }

    /**
     * Removes verification data for the given email
     */
    public void removeVerificationData(String email) {
        verificationData.remove(email);
    }

    /**
     * Extends the verification expiry time for the given email
     */
    public boolean extendVerificationExpiry(String email, int additionalMinutes) {
        VerificationData data = verificationData.get(email);
        if (data == null) {
            return false;
        }

        data.extendExpiry(additionalMinutes);
        return true;
    }

    /**
     * Scheduled task to clean up expired verification data
     * Runs every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    public void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationData.entrySet().removeIf(entry ->
                entry.getValue().isExpired());
    }
}
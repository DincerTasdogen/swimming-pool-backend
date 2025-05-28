package com.sp.SwimmingPool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @InjectMocks
    private VerificationService verificationService;

    private final String testEmail = "test@example.com";

    // Helper method to access the internal map for assertions where necessary,
    // acknowledging this is for testing internals and makes tests more brittle.
    @SuppressWarnings("unchecked")
    private Map<String, Object> getInternalVerificationDataMap(VerificationService service) throws NoSuchFieldException, IllegalAccessException {
        Field field = VerificationService.class.getDeclaredField("verificationData");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(service);
    }

    // Helper method to get a VerificationData object (still an Object due to private inner class)
    private Object getInternalVerificationDataObject(VerificationService service, String email) throws NoSuchFieldException, IllegalAccessException {
        Map<String, Object> internalMap = getInternalVerificationDataMap(service);
        return internalMap.get(email);
    }


    @BeforeEach
    void setUp() {
        // Re-initialize service for each test to ensure a clean state for the internal map
        verificationService = new VerificationService();
    }

    @Test
    void generateAndStoreCode_generates6DigitCodeAndStoresData() {
        Map<String, Object> userData = Map.of("name", "Test User");
        String code = verificationService.generateAndStoreCode(testEmail, userData);

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));

        Map<String, Object> storedData = verificationService.getTempUserData(testEmail);
        assertNotNull(storedData);
        assertEquals("Test User", storedData.get("name"));
        assertTrue(verificationService.verifyCode(testEmail, code));
    }

    @Test
    void generateAndStoreCode_withoutUserData_storesCode() {
        String code = verificationService.generateAndStoreCode(testEmail, null);
        assertNotNull(code);
        assertTrue(verificationService.verifyCode(testEmail, code));
        Map<String, Object> storedData = verificationService.getTempUserData(testEmail);
        assertNotNull(storedData);
        assertTrue(storedData.isEmpty());
    }


    @Test
    void storeTempUserData_forOAuth_storesDataWithoutCode() {
        Map<String, Object> oauthData = Map.of("provider", "google", "id", "123");
        String oauthEmail = "oauth@example.com";
        verificationService.storeTempUserData(oauthEmail, oauthData);

        Map<String, Object> storedData = verificationService.getTempUserData(oauthEmail);
        assertNotNull(storedData);
        assertEquals("google", storedData.get("provider"));
        assertEquals("123", storedData.get("id"));

        assertTrue(verificationService.verifyCode(oauthEmail, "anyCode")); // For OAuth, code doesn't matter
        assertTrue(verificationService.verifyCode(oauthEmail, null));      // Also true with null code
    }

    @Test
    void verifyCode_validCode_notExpired_returnsTrueAndKeepsData() {
        String code = verificationService.generateAndStoreCode(testEmail, null);
        assertTrue(verificationService.verifyCode(testEmail, code));
        assertNotNull(verificationService.getTempUserData(testEmail));
    }

    @Test
    void verifyCode_invalidCode_returnsFalse() {
        String validCode = verificationService.generateAndStoreCode(testEmail, null);
        assertFalse(verificationService.verifyCode(testEmail, "invalidCode"));
        assertNotNull(verificationService.getTempUserData(testEmail));
    }

    @Test
    void verifyCode_emailNotFound_returnsFalse() {
        assertFalse(verificationService.verifyCode("unknown@example.com", "123456"));
    }

    @Test
    void verifyCode_expiredCode_returnsFalseAndRemovesData_simulated() throws Exception {
        String code = verificationService.generateAndStoreCode(testEmail, Map.of("data", "value"));

        // --- Simulation of expiry using Reflection ---
        // This is generally discouraged in pure unit tests but necessary here without a Clock.
        Map<String, Object> internalMap = getInternalVerificationDataMap(verificationService);
        Object verificationDataObject = internalMap.get(testEmail);
        assertNotNull(verificationDataObject, "VerificationData object not found in map for " + testEmail);

        Field expiryTimeField = verificationDataObject.getClass().getDeclaredField("expiryTime");
        expiryTimeField.setAccessible(true);
        // Set expiry time to the past
        expiryTimeField.set(verificationDataObject, LocalDateTime.now().minusMinutes(30));
        // --- End of Reflection ---

        assertFalse(verificationService.verifyCode(testEmail, code), "VerifyCode should return false for expired code");
        assertNull(verificationService.getTempUserData(testEmail), "TempUserData should be null after expired code verification");
        assertTrue(internalMap.isEmpty(), "Internal map should be empty after expired data is removed by verifyCode");
    }


    @Test
    void getTempUserData_dataExistsAndNotExpired_returnsData() {
        Map<String, Object> userData = Map.of("key", "value");
        verificationService.generateAndStoreCode(testEmail, userData);
        Map<String, Object> retrievedData = verificationService.getTempUserData(testEmail);
        assertEquals(userData, retrievedData);
    }

    @Test
    void getTempUserData_dataNotExists_returnsNull() {
        assertNull(verificationService.getTempUserData("unknown@example.com"));
    }

    @Test
    void getTempUserData_expiredData_returnsNull_simulated() throws Exception {
        verificationService.generateAndStoreCode(testEmail, Map.of("data", "value"));

        Map<String, Object> internalMap = getInternalVerificationDataMap(verificationService);
        Object verificationDataObject = internalMap.get(testEmail);
        assertNotNull(verificationDataObject);
        Field expiryTimeField = verificationDataObject.getClass().getDeclaredField("expiryTime");
        expiryTimeField.setAccessible(true);
        expiryTimeField.set(verificationDataObject, LocalDateTime.now().minusMinutes(30)); // Expired

        // getTempUserData itself checks for expiry and returns null
        assertNull(verificationService.getTempUserData(testEmail));
        // The data might still be in the map until cleanup or verifyCode for an expired code is called
        assertNotNull(internalMap.get(testEmail), "Data should still be in map if only getTempUserData was called on expired data");
    }


    @Test
    void updateTempUserData_dataExists_updatesAndReturnsTrue() {
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("name", "Old Name");
        verificationService.generateAndStoreCode(testEmail, initialData);

        Map<String, Object> newData = Map.of("city", "New City", "name", "Updated Name");
        assertTrue(verificationService.updateTempUserData(testEmail, newData));

        Map<String, Object> updatedData = verificationService.getTempUserData(testEmail);
        assertNotNull(updatedData);
        assertEquals("Updated Name", updatedData.get("name"));
        assertEquals("New City", updatedData.get("city"));
    }

    @Test
    void updateTempUserData_dataNotExists_returnsFalse() {
        assertFalse(verificationService.updateTempUserData("unknown@example.com", Map.of("key", "value")));
    }

    @Test
    void updateTempUserData_expiredData_returnsFalse_simulated() throws Exception {
        verificationService.generateAndStoreCode(testEmail, Map.of("data", "value"));

        Map<String, Object> internalMap = getInternalVerificationDataMap(verificationService);
        Object verificationDataObject = internalMap.get(testEmail);
        assertNotNull(verificationDataObject);
        Field expiryTimeField = verificationDataObject.getClass().getDeclaredField("expiryTime");
        expiryTimeField.setAccessible(true);
        expiryTimeField.set(verificationDataObject, LocalDateTime.now().minusMinutes(30)); // Expired

        assertFalse(verificationService.updateTempUserData(testEmail, Map.of("key", "new_value")));
    }


    @Test
    void removeVerificationData_removesData() throws Exception {
        verificationService.generateAndStoreCode(testEmail, null);
        Map<String, Object> internalMap = getInternalVerificationDataMap(verificationService);
        assertFalse(internalMap.isEmpty());

        verificationService.removeVerificationData(testEmail);

        assertNull(verificationService.getTempUserData(testEmail));
        assertTrue(internalMap.isEmpty());
    }

    @Test
    void extendVerificationExpiry_dataExists_extendsExpiryAndReturnsTrue() throws Exception {
        String emailToTest = "extend@example.com";
        verificationService.generateAndStoreCode(emailToTest, null);

        // Get the VerificationData object to inspect its state before extension
        Object dataObjectBeforeExtend = getInternalVerificationDataObject(verificationService, emailToTest);
        assertNotNull(dataObjectBeforeExtend, "VerificationData object not found before extend");
        Field expiryTimeField = dataObjectBeforeExtend.getClass().getDeclaredField("expiryTime");
        expiryTimeField.setAccessible(true);
        LocalDateTime expiryInitiallySet = (LocalDateTime) expiryTimeField.get(dataObjectBeforeExtend);

        int additionalMinutes = 10;
        LocalDateTime timeBeforeExtensionCall = LocalDateTime.now(); // Capture time just before the call
        boolean result = verificationService.extendVerificationExpiry(emailToTest, additionalMinutes);
        LocalDateTime timeAfterExtensionCall = LocalDateTime.now(); // Capture time just after the call

        assertTrue(result, "extendVerificationExpiry should return true for existing data");

        Object dataObjectAfterExtend = getInternalVerificationDataObject(verificationService, emailToTest);
        assertNotNull(dataObjectAfterExtend, "VerificationData object not found after extend");
        LocalDateTime expiryAfterExtend = (LocalDateTime) expiryTimeField.get(dataObjectAfterExtend);

        assertNotNull(expiryAfterExtend, "Expiry time should not be null after extension");

        LocalDateTime expectedMinimumExpiry = timeBeforeExtensionCall.plusMinutes(additionalMinutes);
        LocalDateTime expectedMaximumExpiry = timeAfterExtensionCall.plusMinutes(additionalMinutes).plusSeconds(1);

        assertFalse(expiryAfterExtend.isBefore(expectedMinimumExpiry.minusSeconds(1)), "Expiry time " + expiryAfterExtend + " should be around " + additionalMinutes + " minutes from the call time (expected min: " + expectedMinimumExpiry + ")");
        assertFalse(expiryAfterExtend.isAfter(expectedMaximumExpiry), "Expiry time " + expiryAfterExtend + " should be around " + additionalMinutes + " minutes from the call time (expected max: " + expectedMaximumExpiry + ")");

    }

    @Test
    void extendVerificationExpiry_dataNotExists_returnsFalse() {
        assertFalse(verificationService.extendVerificationExpiry("unknown@example.com", 5));
    }

    @Test
    void cleanupExpiredCodes_removesOnlyExpiredData_simulated() throws Exception {
        String expiredEmail = "expired@example.com";
        String currentEmail = "current@example.com";

        // Add current data
        verificationService.generateAndStoreCode(currentEmail, Map.of("status", "current"));

        // Add data that we will manually mark as expired via reflection
        verificationService.generateAndStoreCode(expiredEmail, Map.of("status", "toBeExpired"));
        Map<String, Object> internalMap = getInternalVerificationDataMap(verificationService);
        Object expiredDataObject = internalMap.get(expiredEmail);
        assertNotNull(expiredDataObject, "Data for expiredEmail not found before marking expired");
        Field expiryTimeField = expiredDataObject.getClass().getDeclaredField("expiryTime");
        expiryTimeField.setAccessible(true);
        expiryTimeField.set(expiredDataObject, LocalDateTime.now().minusMinutes(30)); // Mark as expired

        assertEquals(2, internalMap.size(), "Map should have two entries before cleanup");

        verificationService.cleanupExpiredCodes();

        assertEquals(1, internalMap.size(), "Map should have one entry after cleanup");
        assertNull(verificationService.getTempUserData(expiredEmail), "Expired data should be removed");
        assertNotNull(verificationService.getTempUserData(currentEmail), "Current data should remain");
        assertNotNull(internalMap.get(currentEmail), "Current data should still be in internal map");
        assertNull(internalMap.get(expiredEmail), "Expired data should be removed from internal map");
    }

    @Test
    void cleanupExpiredCodes_noExpiredData_noChange() throws Exception {
        verificationService.generateAndStoreCode(testEmail, Map.of("status", "current"));
        String anotherEmail = "another@example.com";
        verificationService.generateAndStoreCode(anotherEmail, Map.of("status", "also_current"));

        Map<String, Object> internalMap = getInternalVerificationDataMap(verificationService);
        assertEquals(2, internalMap.size());

        verificationService.cleanupExpiredCodes();

        assertEquals(2, internalMap.size());
        assertNotNull(verificationService.getTempUserData(testEmail));
        assertNotNull(verificationService.getTempUserData(anotherEmail));
    }
}
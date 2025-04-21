package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.service.StorageService;
import com.sp.SwimmingPool.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class DocumentUploadController {

    private final StorageService storageService;
    private final VerificationService verificationService;

    @Autowired
    public DocumentUploadController(StorageService storageService, VerificationService verificationService) {
        this.storageService = storageService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register/upload-documents")
    public ResponseEntity<?> uploadRegistrationDocuments(
            @RequestParam("email") String email,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "idPhotoFront", required = false) MultipartFile idPhotoFront,
            @RequestParam(value = "idPhotoBack", required = false) MultipartFile idPhotoBack) {

        try {
            // Validate that we have a registration in progress
            Map<String, Object> userData = verificationService.getTempUserData(email);
            if (userData == null) {
                return ResponseEntity.badRequest().body("No registration in progress for this email");
            }

            Map<String, Object> documentsData = new HashMap<>();

            // Store each file if provided - use regular storage for now (these will be migrated later)
            if (photo != null && !photo.isEmpty()) {
                validateImage(photo);
                String photoPath = storageService.storeFile(photo, "registration/biometric");
                documentsData.put("photo", photoPath);
            }

            if (idPhotoFront != null && !idPhotoFront.isEmpty()) {
                validateImage(idPhotoFront);
                String idFrontPath = storageService.storeFile(idPhotoFront, "registration/id");
                documentsData.put("idPhotoFront", idFrontPath);
            }

            if (idPhotoBack != null && !idPhotoBack.isEmpty()) {
                validateImage(idPhotoBack);
                String idBackPath = storageService.storeFile(idPhotoBack, "registration/id");
                documentsData.put("idPhotoBack", idBackPath);
            }

            // Update the temporary user data with file paths
            boolean updated = verificationService.updateTempUserData(email, documentsData);
            if (!updated) {
                return ResponseEntity.badRequest().body("Failed to update user data");
            }

            // Extend verification expiry time to give more time to complete registration
            verificationService.extendVerificationExpiry(email, 15); // Give 15 more minutes

            return ResponseEntity.ok(documentsData);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload files: " + e.getMessage());
        }
    }

    /**
     * After member registration is completed, this method can be called to migrate
     * registration photos to the member-specific directories
     */
    @PostMapping("/register/complete/{memberId}")
    public ResponseEntity<?> completeRegistration(@PathVariable int memberId, @RequestParam String email) {
        try {
            // Get temporary user data from verification service
            Map<String, Object> userData = verificationService.getTempUserData(email);
            if (userData == null) {
                return ResponseEntity.badRequest().body("No registration data found for this email");
            }

            // Map to collect migration results
            Map<String, String> migratedPaths = new HashMap<>();

            // Migrate profile photo if exists
            if (userData.containsKey("photo")) {
                String oldPath = (String) userData.get("photo");
                migratePhotoToMemberDirectory(oldPath, memberId, "profile", migratedPaths);
            }

            // Migrate ID front photo if exists
            if (userData.containsKey("idPhotoFront")) {
                String oldPath = (String) userData.get("idPhotoFront");
                migratePhotoToMemberDirectory(oldPath, memberId, "id/front", migratedPaths);
            }

            // Migrate ID back photo if exists
            if (userData.containsKey("idPhotoBack")) {
                String oldPath = (String) userData.get("idPhotoBack");
                migratePhotoToMemberDirectory(oldPath, memberId, "id/back", migratedPaths);
            }

            // You might want to clean up temporary data after successful migration

            return ResponseEntity.ok(migratedPaths);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to complete registration: " + e.getMessage());
        }
    }

    /**
     * Helper method to migrate a photo from registration directory to member directory
     */
    private void migratePhotoToMemberDirectory(String oldPath, int memberId, String photoType, Map<String, String> results) {
        try {
            // We'll implement a manual copy for now - in a real application you'd want
            // to use the AmazonS3 copyObject method to avoid downloading/uploading

            // Load the existing file
            Resource resource = storageService.loadFileAsResource(oldPath);

            // Get file extension from old path
            String extension = "";
            int lastDotIndex = oldPath.lastIndexOf('.');
            if (lastDotIndex != -1) {
                extension = oldPath.substring(lastDotIndex);
            }

            // Create a MultipartFile from the resource (this would need a custom implementation)
            // For now, we'll assume this is possible - in production you'd use S3's copyObject

            // Store in the new location
            String newPath = storageService.storeMemberFile(
                    convertResourceToMultipartFile(resource, extension), photoType, memberId);

            // Add to results
            results.put(photoType, newPath);

            // Optionally delete the old file
            // storageService.deleteFile(oldPath);

        } catch (Exception e) {
            // Log error but continue with other files
            System.err.println("Failed to migrate photo " + oldPath + ": " + e.getMessage());
        }
    }

    /**
     * This is a placeholder - you'd need to implement a way to convert Resource to MultipartFile
     * In a real application, you'd use S3's copyObject method instead
     */
    private MultipartFile convertResourceToMultipartFile(Resource resource, String extension) {
        // This is just a placeholder
        throw new UnsupportedOperationException("This method needs a proper implementation");
    }

    private void validateImage(MultipartFile file) {
        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit (5MB)");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
    }
}
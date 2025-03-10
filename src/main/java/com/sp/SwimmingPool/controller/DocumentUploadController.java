package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.service.StorageService;
import com.sp.SwimmingPool.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
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

            // Store each file if provided
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
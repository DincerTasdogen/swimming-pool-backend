package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.security.UserPrincipal;
import com.sp.SwimmingPool.service.MemberService;
import com.sp.SwimmingPool.service.S3StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/member-photos")
public class MemberPhotoController {

    private final S3StorageService storageService;
    private final MemberService memberService;

    @Autowired
    public MemberPhotoController(S3StorageService storageService, MemberService memberService) {
        this.storageService = storageService;
        this.memberService = memberService;
    }

    /**
     * Upload a member's profile photo
     */
    @PostMapping("/{memberId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'COACH') or (hasRole('MEMBER') and #memberId == authentication.principal.id)")
    public ResponseEntity<?> uploadProfilePhoto(
            @PathVariable int memberId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            // Verify member exists
            memberService.getMemberDetails(memberId);

            // Validate image
            validateImage(file);

            // Store file in the profile directory for this member
            String filePath = storageService.storeMemberFile(file, "profile", memberId);

            // Update the member record with the new photo path
            updateMemberProfilePhoto(memberId, filePath);

            Map<String, String> response = new HashMap<>();
            response.put("path", filePath);
            response.put("url", storageService.getFileUrl(filePath));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload profile photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload a member's ID photos (front and back)
     */
    @PostMapping("/{memberId}/id")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'COACH') or (hasRole('MEMBER') and #memberId == authentication.principal.id)")
    public ResponseEntity<?> uploadIdPhotos(
            @PathVariable int memberId,
            @RequestParam(value = "front", required = false) MultipartFile frontFile,
            @RequestParam(value = "back", required = false) MultipartFile backFile) {

        try {
            // Verify member exists
            MemberDTO memberDTO = memberService.getMemberDetails(memberId);

            Map<String, String> response = new HashMap<>();

            // Upload front ID photo if provided
            if (frontFile != null && !frontFile.isEmpty()) {
                validateImage(frontFile);
                String frontPath = storageService.storeMemberFile(frontFile, "id/front", memberId);
                memberDTO.setIdPhotoFront(frontPath);
                response.put("frontPath", frontPath);
                response.put("frontUrl", storageService.getFileUrl(frontPath));
            }

            // Upload back ID photo if provided
            if (backFile != null && !backFile.isEmpty()) {
                validateImage(backFile);
                String backPath = storageService.storeMemberFile(backFile, "id/back", memberId);
                memberDTO.setIdPhotoBack(backPath);
                response.put("backPath", backPath);
                response.put("backUrl", storageService.getFileUrl(backPath));
            }

            // Update member record with new photo paths
            memberService.updateMember(memberId, memberDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload ID photos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload a member's medical photo
     */
    @PostMapping("/{memberId}/medical")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<?> uploadMedicalPhoto(
            @PathVariable int memberId,
            @RequestParam("file") MultipartFile file) {

        try {
            // Verify member exists
            memberService.getMemberDetails(memberId);

            // Validate image
            validateImage(file);

            // Store file in the medical directory for this member
            String filePath = storageService.storeMemberFile(file, "medical", memberId);

            Map<String, String> response = new HashMap<>();
            response.put("path", filePath);
            response.put("url", storageService.getFileUrl(filePath));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload medical photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get a member's photo
     * Access control:
     * - Profile, ID photos: Accessible by the member and staff roles (Admin, Doctor, Coach)
     * - Medical photos: Only accessible by Admin and Doctor roles
     */
    @GetMapping("/{memberId}/{photoType}")
    public ResponseEntity<Resource> getMemberPhoto(
            @PathVariable int memberId,
            @PathVariable String photoType,
            Authentication authentication) {

        try {
            // Check role-based permissions for medical photos
            if ("medical".equals(photoType)) {
                if (!hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_DOCTOR")) {
                    return ResponseEntity.status(403).build(); // Forbidden
                }
            } else {
                // For non-medical photos, check if current user is the member or a staff role
                if (!hasAccessToMemberPhoto(authentication, memberId)) {
                    return ResponseEntity.status(403).build(); // Forbidden
                }
            }

            // Determine the full file path based on photo type
            String filePath = determineMemberPhotoPath(memberId, photoType);

            // Load file as Resource
            Resource resource = storageService.loadFileAsResource(filePath);

            // Determine content type
            String contentType = determineContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a member's photo
     */
    @DeleteMapping("/{memberId}/{photoType}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MEMBER') and #memberId == authentication.principal.id)")
    public ResponseEntity<?> deleteMemberPhoto(
            @PathVariable int memberId,
            @PathVariable String photoType) {

        try {
            // Check if medical photo (only admin can delete these)
            if ("medical".equals(photoType) && !hasRole(SecurityContextHolder.getContext().getAuthentication(), "ROLE_ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("error", "Only admins can delete medical photos"));
            }

            // Get the file path
            String filePath = determineMemberPhotoPath(memberId, photoType);

            // Delete the file
            storageService.deleteFile(filePath);

            // If it's a profile or ID photo, update the member record
            if ("profile".equals(photoType) || photoType.startsWith("id")) {
                clearMemberPhotoField(memberId, photoType);
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Photo deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * List all photos for a member
     */
    @GetMapping("/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'COACH') or (hasRole('MEMBER') and #memberId == authentication.principal.id)")
    public ResponseEntity<?> listMemberPhotos(@PathVariable int memberId, Authentication authentication) {
        try {
            // Verify member exists
            memberService.getMemberDetails(memberId);

            Map<String, String> photoUrls = new HashMap<>();

            // Define the photo types to check based on role
            String[] photoTypes;
            if (hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_DOCTOR")) {
                photoTypes = new String[]{"profile", "id/front", "id/back", "medical"};
            } else {
                // Coach and Member can't see medical photos
                photoTypes = new String[]{"profile", "id/front", "id/back"};
            }

            // Check each photo type
            for (String type : photoTypes) {
                String path = determineMemberPhotoPath(memberId, type);
                try {
                    // Try to access the file to see if it exists
                    storageService.loadFileAsResource(path);
                    photoUrls.put(type.replace("/", "_"), storageService.getFileUrl(path));
                } catch (IOException e) {
                    // Photo doesn't exist or can't be accessed, skip it
                }
            }

            return ResponseEntity.ok(photoUrls);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Determine the file path for a member's photo based on type
     */
    private String determineMemberPhotoPath(int memberId, String photoType) {
        if (photoType.equals("profile")) {
            return "members/" + memberId + "/profile";
        } else if (photoType.equals("id/front") || photoType.equals("idFront")) {
            return "members/" + memberId + "/id/front";
        } else if (photoType.equals("id/back") || photoType.equals("idBack")) {
            return "members/" + memberId + "/id/back";
        } else if (photoType.equals("medical")) {
            return "members/" + memberId + "/medical";
        } else {
            return "members/" + memberId + "/" + photoType;
        }
    }

    /**
     * Update the profile photo field in the member record
     */
    private void updateMemberProfilePhoto(int memberId, String filePath) {
        try {
            MemberDTO memberDTO = memberService.getMemberDetails(memberId);
            memberDTO.setPhoto(filePath);
            memberService.updateMember(memberId, memberDTO);
        } catch (Exception e) {
            // Log error but don't fail the upload
            System.err.println("Failed to update member photo field: " + e.getMessage());
        }
    }

    /**
     * Clear a photo field in the member record
     */
    private void clearMemberPhotoField(int memberId, String photoType) {
        try {
            MemberDTO memberDTO = memberService.getMemberDetails(memberId);

            if ("profile".equals(photoType)) {
                memberDTO.setPhoto(null);
            } else if ("id/front".equals(photoType) || "idFront".equals(photoType)) {
                memberDTO.setIdPhotoFront(null);
            } else if ("id/back".equals(photoType) || "idBack".equals(photoType)) {
                memberDTO.setIdPhotoBack(null);
            }

            memberService.updateMember(memberId, memberDTO);
        } catch (Exception e) {
            // Log error but don't fail the delete operation
            System.err.println("Failed to clear member photo field: " + e.getMessage());
        }
    }

    /**
     * Check if the current user has access to a member's photo
     */
    private boolean hasAccessToMemberPhoto(Authentication authentication, int memberId) {
        // Staff roles have access to all member photos
        if (hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_COACH")) {
            return true;
        }

        // Members can only access their own photos
        if (hasRole(authentication, "ROLE_MEMBER")) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId() == memberId;
        }

        return false;
    }

    /**
     * Check if the authenticated user has the specified role
     */
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    /**
     * Check if the authenticated user has any of the specified roles
     */
    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (String role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that the uploaded file is an image and within size limits
     */
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

    /**
     * Determine the content type of a file based on its extension
     */
    private String determineContentType(String filePath) {
        if (filePath.lastIndexOf(".") == -1) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            default:
                return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
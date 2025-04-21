package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/pool-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadPoolImage(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = storageService.storeFile(file, "pools");

            Map<String, String> response = new HashMap<>();
            response.put("path", filePath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/files/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        try {
            // Extract full path from request
            String requestPath = (String) request.getAttribute(
                    HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String filePath = requestPath.substring("/api/upload/files/".length());

            if (!storageService.hasAccessToFile(filePath)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            // Load file as Resource
            Resource resource = storageService.loadFileAsResource(filePath);

            // Determine content type
            String contentType = determineContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                            filePath.substring(filePath.lastIndexOf("/") + 1) + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/files/**")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFile(HttpServletRequest request) {
        try {
            // Extract full path from request
            String requestPath = (String) request.getAttribute(
                    HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String filePath = requestPath.substring("/api/upload/files/".length());

            storageService.deleteFile(filePath);
            Map<String, String> response = new HashMap<>();
            response.put("message", "File deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

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
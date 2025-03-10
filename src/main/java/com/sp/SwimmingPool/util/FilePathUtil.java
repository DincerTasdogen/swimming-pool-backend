package com.sp.SwimmingPool.util;

import com.sp.SwimmingPool.service.S3StorageService;
import com.sp.SwimmingPool.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FilePathUtil {

    private final StorageService storageService;

    @Value("${app.storage.type:s3}")
    private String storageType;

    @Value("${app.base-url:/api}")
    private String baseUrl;

    public FilePathUtil(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Converts a stored file path to a full URL path for client access
     * @param filePath the stored path in the database
     * @return full URL path for the client
     */
    public String getFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // If S3 storage is being used and it's a public directory, use the S3 URL
        if (storageType.equalsIgnoreCase("s3") && storageService instanceof S3StorageService) {
            return ((S3StorageService) storageService).getFileUrl(filePath);
        }

        // For local storage or private S3 files, use the API path
        return baseUrl + "/upload/files/" + filePath;
    }
}
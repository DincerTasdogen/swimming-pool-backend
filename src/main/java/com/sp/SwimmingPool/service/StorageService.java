package com.sp.SwimmingPool.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    /**
     * Store a file in the specified directory
     */
    String storeFile(MultipartFile file, String directory) throws IOException;

    /**
     * Store a file for a specific member
     */
    String storeMemberFile(MultipartFile file, String directory, int memberId) throws IOException;

    /**
     * Load a file as a resource
     */
    Resource loadFileAsResource(String fileName) throws IOException;

    /**
     * Delete a file
     */
    void deleteFile(String fileName) throws IOException;

    /**
     * Get the URL for a file
     */
    String getFileUrl(String filePath);

    /**
     * Check if the current user has access to the specified file
     */
    boolean hasAccessToFile(String filePath);
}
package com.sp.SwimmingPool.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    /**
     * Store a file in the storage system
     *
     * @param file The file to store
     * @param directory The directory to store the file in
     * @return The path to the stored file
     * @throws IOException If an error occurs during file storage
     */
    String storeFile(MultipartFile file, String directory) throws IOException;

    /**
     * Load a file as a resource
     *
     * @param filePath The name of the file to load
     * @return The file as a resource
     * @throws IOException If the file cannot be loaded
     */
    Resource loadFileAsResource(String filePath) throws IOException;

    /**
     * Delete a file from storage
     *
     * @param filePath The name of the file to delete
     * @throws IOException If the file cannot be deleted
     */
    void deleteFile(String filePath) throws IOException;
}
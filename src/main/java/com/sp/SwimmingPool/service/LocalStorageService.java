package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService {

    private final Path fileStorageLocation;

    public LocalStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new StorageException("Could not create the directory where the uploaded files will be stored", ex);
        }
    }

    public String storeFile(MultipartFile file, String directory) {
        validateFile(file);

        // Create subdirectory if it doesn't exist
        Path targetLocation = this.fileStorageLocation.resolve(directory);
        try {
            Files.createDirectories(targetLocation);
        } catch (IOException ex) {
            throw new StorageException("Could not create directory: " + directory, ex);
        }

        // Generate file name and save
        String fileName = generateFileName(file);
        try {
            Path targetPath = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return directory + "/" + fileName;
        } catch (IOException ex) {
            throw new StorageException("Could not store file " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new StorageException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new StorageException("File not found: " + fileName, ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                return;
            }

            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!Files.exists(filePath)) {
                throw new StorageException("File not found: " + fileName);
            }

            Files.delete(filePath);

            // Try to delete empty parent directory
            Path parentDir = filePath.getParent();
            if (Files.isDirectory(parentDir) && isDirectoryEmpty(parentDir)) {
                Files.delete(parentDir);
            }
        } catch (IOException ex) {
            throw new StorageException("Could not delete file: " + fileName, ex);
        }
    }

    private boolean isDirectoryEmpty(Path directory) {
        try {
            return Files.list(directory).findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        // Validate file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (fileName.contains("..")) {
            throw new StorageException("Filename contains invalid path sequence: " + fileName);
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !(
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("application/pdf"))) {
            throw new StorageException("Invalid file type. Only JPEG, PNG and PDF files are allowed");
        }

        // Validate file size (5MB)
        if (file.getSize() > 5_000_000) {
            throw new StorageException("File size exceeds maximum limit of 5MB");
        }
    }

    private String generateFileName(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFileName);
        return UUID.randomUUID().toString() + "." + extension;
    }
}
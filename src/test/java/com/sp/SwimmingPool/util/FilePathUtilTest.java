package com.sp.SwimmingPool.util;

import com.sp.SwimmingPool.service.S3StorageService;
import com.sp.SwimmingPool.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilePathUtilTest {

    @Mock
    private StorageService storageService; // Generic storage service

    @Mock
    private S3StorageService s3StorageService; // Specific S3 storage service

    private FilePathUtil filePathUtil;

    @BeforeEach
    void setUp() {
        // Initialize FilePathUtil with the generic storageService mock
        // We will swap it or cast it in specific tests if needed
        filePathUtil = new FilePathUtil(storageService);
        ReflectionTestUtils.setField(filePathUtil, "baseUrl", "/api/base");
    }

    @Test
    void getFileUrl_shouldReturnNull_whenFilePathIsNullOrEmpty() {
        assertNull(filePathUtil.getFileUrl(null));
        assertNull(filePathUtil.getFileUrl(""));
    }

    @Test
    void getFileUrl_shouldUseS3Url_whenStorageTypeIsS3AndServiceIsS3Instance() {
        // Re-initialize or set field for this specific scenario
        filePathUtil = new FilePathUtil(s3StorageService); // Use s3StorageService instance
        ReflectionTestUtils.setField(filePathUtil, "storageType", "s3");
        ReflectionTestUtils.setField(filePathUtil, "baseUrl", "/api/base");

        String filePath = "public/image.jpg";
        String expectedS3Url = "https://s3.example.com/public/image.jpg";
        when(s3StorageService.getFileUrl(filePath)).thenReturn(expectedS3Url);

        String actualUrl = filePathUtil.getFileUrl(filePath);

        assertEquals(expectedS3Url, actualUrl);
    }

    @Test
    void getFileUrl_shouldUseApiUrl_whenStorageTypeIsS3ButServiceIsNotS3Instance() {
        // filePathUtil is already initialized with generic storageService
        ReflectionTestUtils.setField(filePathUtil, "storageType", "s3");
        String filePath = "private/document.pdf";
        String expectedApiUrl = "/api/base/upload/files/" + filePath;

        String actualUrl = filePathUtil.getFileUrl(filePath);

        assertEquals(expectedApiUrl, actualUrl);
    }

    @Test
    void getFileUrl_shouldUseApiUrl_whenStorageTypeIsLocal() {
        ReflectionTestUtils.setField(filePathUtil, "storageType", "local");
        String filePath = "uploads/file.txt";
        String expectedApiUrl = "/api/base/upload/files/" + filePath;

        String actualUrl = filePathUtil.getFileUrl(filePath);

        assertEquals(expectedApiUrl, actualUrl);
    }

    @Test
    void getFileUrl_shouldUseApiUrl_whenStorageTypeIsS3AndFilePathIsPrivate() {
        filePathUtil = new FilePathUtil(s3StorageService);
        ReflectionTestUtils.setField(filePathUtil, "storageType", "s3");
        ReflectionTestUtils.setField(filePathUtil, "baseUrl", "/api/base");

        String filePath = "private-s3/confidential.docx";
        String s3DirectUrlForPrivate = "https://s3.example.com/private-s3/confidential.docx"; // or a presigned URL
        when(s3StorageService.getFileUrl(filePath)).thenReturn(s3DirectUrlForPrivate);


        String actualUrl = filePathUtil.getFileUrl(filePath);
        assertEquals(s3DirectUrlForPrivate, actualUrl);
    }
}
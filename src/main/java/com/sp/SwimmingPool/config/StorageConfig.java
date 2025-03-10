package com.sp.SwimmingPool.config;

import com.sp.SwimmingPool.service.LocalStorageService;
import com.sp.SwimmingPool.service.S3StorageService;
import com.sp.SwimmingPool.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

    @Value("${app.storage.type:s3}")
    private String storageType;

    @Bean
    @Primary
    public StorageService storageService(LocalStorageService localStorageService,
                                         S3StorageService s3StorageService) {
        if ("s3".equalsIgnoreCase(storageType)) {
            return s3StorageService;
        } else {
            return localStorageService;
        }
    }
}
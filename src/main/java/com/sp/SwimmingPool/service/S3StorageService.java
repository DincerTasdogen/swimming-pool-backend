package com.sp.SwimmingPool.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class S3StorageService implements StorageService {

    private final AmazonS3 s3Client;
    private final Set<String> publicDirectories = new HashSet<>();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.endpoint-url}")
    private String endpointUrl;

    public S3StorageService(
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey,
            @Value("${aws.s3.region}") String region) {

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        this.s3Client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.fromName(region))
                .build();

        // Define which directories should be public
        publicDirectories.add("pools");
        // Add other public directories as needed
    }

    @Override
    public String storeFile(MultipartFile file, String directory) throws IOException {
        // Generate unique file name
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String fileName = directory + "/" + UUID.randomUUID() + extension;

        // Set metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        // Create the request
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                fileName,
                file.getInputStream(),
                metadata);

        // Only make public if in the public directories list
        if (isPublicDirectory(directory)) {
            putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        }

        // Upload to S3
        s3Client.putObject(putObjectRequest);

        // Return the file path/URL
        return fileName;
    }

    @Override
    public Resource loadFileAsResource(String fileName) throws IOException {
        try {
            // Get S3 object
            byte[] content = s3Client.getObject(bucketName, fileName)
                    .getObjectContent()
                    .readAllBytes();

            return new ByteArrayResource(content);
        } catch (Exception e) {
            throw new IOException("File not found: " + fileName, e);
        }
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        try {
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        } catch (Exception e) {
            throw new IOException("Error deleting file: " + fileName, e);
        }
    }

    public String getFileUrl(String filePath) {
        // For public files, return the direct S3 URL
        if (isPublicFile(filePath)) {
            return endpointUrl + "/" + bucketName + "/" + filePath;
        }

        // For private files, return a path that will go through our controller
        return "/api/upload/files/" + filePath;
    }

    private boolean isPublicDirectory(String directory) {
        return publicDirectories.contains(directory);
    }

    private boolean isPublicFile(String filePath) {
        // Check if the file is in a public directory
        for (String dir : publicDirectories) {
            if (filePath.startsWith(dir + "/")) {
                return true;
            }
        }
        return false;
    }
}
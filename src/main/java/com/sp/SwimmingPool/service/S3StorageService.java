package com.sp.SwimmingPool.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.sp.SwimmingPool.model.entity.RegistrationFile;
import com.sp.SwimmingPool.repos.RegistrationFileRepository;
import com.sp.SwimmingPool.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class S3StorageService implements StorageService {

    private final AmazonS3 s3Client;
    private final Set<String> publicDirectories = new HashSet<>();
    private final Set<String> memberPrivateDirectories = new HashSet<>();
    private final RegistrationFileRepository registrationFileRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.endpoint-url}")
    private String endpointUrl;

    public S3StorageService(
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey,
            @Value("${aws.s3.region}") String region, RegistrationFileRepository registrationFileRepository) {

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        this.s3Client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.fromName(region))
                .build();

        // Define which directories should be public
        publicDirectories.add("pools");
        publicDirectories.add("news");
        publicDirectories.add("public");

        // Define which directories should be member-private (require role-based access control)
        memberPrivateDirectories.add("members");
        memberPrivateDirectories.add("registration/biometric");
        memberPrivateDirectories.add("registration/id");
        this.registrationFileRepository = registrationFileRepository;
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

    /**
     * Store a file for a specific member
     */
    public String storeMemberFile(MultipartFile file, String photoType, int memberId) throws IOException {
        String memberPath = "members/" + memberId + "/" + photoType;
        return storeFile(file, memberPath);
    }

    @Override
    public Resource loadFileAsResource(String fileName) throws IOException {
        try {
            // Check if the current user has access to this file
            if (!hasAccessToFile(fileName)) {
                throw new IOException("Access denied to file: " + fileName);
            }

            // Get S3 object
            byte[] content = s3Client.getObject(bucketName, fileName)
                    .getObjectContent()
                    .readAllBytes();

            return new ByteArrayResource(content);
        } catch (Exception e) {
            throw new IOException("File not found or access denied: " + fileName, e);
        }
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        try {
            // Check if the current user has access to delete this file
            if (!hasAccessToFile(fileName)) {
                throw new IOException("Access denied to delete file: " + fileName);
            }

            s3Client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        } catch (Exception e) {
            throw new IOException("Error deleting file: " + fileName, e);
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        // For public files, return the direct S3 URL
        if (isPublicFile(filePath)) {
            return endpointUrl + "/" + bucketName + "/" + filePath;
        }

        // For private files, return a path that will go through our controller
        return filePath;
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

    /**
     * Checks if a file path is in a member-private directory
     */
    private boolean isMemberPrivateFile(String filePath) {
        for (String dir : memberPrivateDirectories) {
            if (filePath.startsWith(dir + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract member ID from file path (if possible)
     */
    private Integer extractMemberId(String filePath) {
        // For paths like "members/123/profile/abc.jpg"
        if (filePath.startsWith("members/")) {
            String[] parts = filePath.split("/");
            if (parts.length >= 3) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        // For registration paths, we don't have member ID in the path
        // We would need additional logic to map registration files to member IDs
        // For now, we'll rely on the role-based security for these files

        return null;
    }

    /**
     * Check if the current user has access to the specified file
     */
    public boolean hasAccessToFile(String filePath) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Public files are accessible to everyone
        if (isPublicFile(filePath)) {
            return true;
        }

        // Staff roles (Admin, Doctor, Coach) have access to all files
        if (hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_COACH")) {
            return true;
        }

        // For member files, members can only access their own files
        if (isMemberPrivateFile(filePath) && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MEMBER"))) {
            Optional<RegistrationFile> fileOpt = registrationFileRepository.findByS3Key(filePath);
            if (fileOpt.isPresent()) {
                RegistrationFile file = fileOpt.get();
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                Integer currentMemberId = userPrincipal.getId();
                return file.getMember().getId() == currentMemberId;
            }
            return false;
        }

        // For any other file or situation, deny access
        return false;
    }

    /**
     * Check if the authenticated user has any of the specified roles
     */
    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (String role : roles) {
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority(role))) {
                return true;
            }
        }
        return false;
    }
    /**
     * Copy an object within the same S3 bucket
     */
    public boolean copyS3Object(String sourceKey, String destinationKey) {
        try {
            // Check if source object exists
            if (!s3Client.doesObjectExist(bucketName, sourceKey)) {
                System.err.println("Source object does not exist: " + sourceKey);
                return false;
            }

            // Copy the object
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, sourceKey, bucketName, destinationKey);
            s3Client.copyObject(copyRequest);

            // Verify the copy was successful
            boolean success = s3Client.doesObjectExist(bucketName, destinationKey);

            return success;
        } catch (Exception e) {
            System.err.println("Error copying object from " + sourceKey + " to " + destinationKey + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete an object from S3
     */
    public boolean deleteS3Object(String key) {
        try {
            s3Client.deleteObject(bucketName, key);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting object: " + key + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * List all objects in an S3 directory
     */
    public List<String> listS3Objects(String prefix) {
        List<String> objects = new ArrayList<>();

        ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix);

        ListObjectsV2Result result;
        do {
            result = s3Client.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                objects.add(objectSummary.getKey());
            }

            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return objects;
    }
}
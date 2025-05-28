package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.RegistrationFile;
import com.sp.SwimmingPool.repos.RegistrationFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberFileServiceTest {

    @Mock
    private RegistrationFileRepository registrationFileRepository;

    @InjectMocks
    private MemberFileService memberFileService;

    private RegistrationFile biometricFile;
    private RegistrationFile idFrontFile;
    private RegistrationFile idBackFile;
    private final int memberId = 101;

    @BeforeEach
    void setUp() {
        biometricFile = RegistrationFile.builder()
                .id(1L)
                .s3Key("s3://bucket/members/101/biometric/photo.jpg")
                .member(null) // In a real scenario, this would be the Member entity
                .type("biometric")
                .uploadedAt(LocalDateTime.now())
                .build();

        idFrontFile = RegistrationFile.builder()
                .id(2L)
                .s3Key("s3://bucket/members/101/id/front.png")
                .member(null)
                .type("id-front")
                .uploadedAt(LocalDateTime.now())
                .build();

        idBackFile = RegistrationFile.builder()
                .id(3L)
                .s3Key("s3://bucket/members/101/id/back.jpeg")
                .member(null)
                .type("id-back")
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getBiometricPhotoPath_fileExists_returnsOptionalOfPath() {
        when(registrationFileRepository.findFirstByMemberIdAndType(memberId, "biometric"))
                .thenReturn(Optional.of(biometricFile));

        Optional<String> result = memberFileService.getBiometricPhotoPath(memberId);

        assertTrue(result.isPresent());
        assertEquals(biometricFile.getS3Key(), result.get());
        verify(registrationFileRepository).findFirstByMemberIdAndType(memberId, "biometric");
    }

    @Test
    void getBiometricPhotoPath_fileNotExists_returnsEmptyOptional() {
        when(registrationFileRepository.findFirstByMemberIdAndType(memberId, "biometric"))
                .thenReturn(Optional.empty());

        Optional<String> result = memberFileService.getBiometricPhotoPath(memberId);

        assertTrue(result.isEmpty());
        verify(registrationFileRepository).findFirstByMemberIdAndType(memberId, "biometric");
    }

    @Test
    void getIdPhotoFrontPath_fileExists_returnsOptionalOfPath() {
        when(registrationFileRepository.findFirstByMemberIdAndType(memberId, "id-front"))
                .thenReturn(Optional.of(idFrontFile));

        Optional<String> result = memberFileService.getIdPhotoFrontPath(memberId);

        assertTrue(result.isPresent());
        assertEquals(idFrontFile.getS3Key(), result.get());
        verify(registrationFileRepository).findFirstByMemberIdAndType(memberId, "id-front");
    }

    @Test
    void getIdPhotoFrontPath_fileNotExists_returnsEmptyOptional() {
        when(registrationFileRepository.findFirstByMemberIdAndType(memberId, "id-front"))
                .thenReturn(Optional.empty());

        Optional<String> result = memberFileService.getIdPhotoFrontPath(memberId);

        assertTrue(result.isEmpty());
        verify(registrationFileRepository).findFirstByMemberIdAndType(memberId, "id-front");
    }

    @Test
    void getIdPhotoBackPath_fileExists_returnsOptionalOfPath() {
        when(registrationFileRepository.findFirstByMemberIdAndType(memberId, "id-back"))
                .thenReturn(Optional.of(idBackFile));

        Optional<String> result = memberFileService.getIdPhotoBackPath(memberId);

        assertTrue(result.isPresent());
        assertEquals(idBackFile.getS3Key(), result.get());
        verify(registrationFileRepository).findFirstByMemberIdAndType(memberId, "id-back");
    }

    @Test
    void getIdPhotoBackPath_fileNotExists_returnsEmptyOptional() {
        when(registrationFileRepository.findFirstByMemberIdAndType(memberId, "id-back"))
                .thenReturn(Optional.empty());

        Optional<String> result = memberFileService.getIdPhotoBackPath(memberId);

        assertTrue(result.isEmpty());
        verify(registrationFileRepository).findFirstByMemberIdAndType(memberId, "id-back");
    }
}
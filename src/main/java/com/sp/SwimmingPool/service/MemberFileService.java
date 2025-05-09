package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.RegistrationFile;
import com.sp.SwimmingPool.repos.RegistrationFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class MemberFileService {

    private final RegistrationFileRepository registrationFileRepository;

    public Optional<String> getBiometricPhotoPath(int memberId) {
        return registrationFileRepository
                .findFirstByMemberIdAndType(memberId, "biometric")
                .map(RegistrationFile::getS3Key);
    }

    public Optional<String> getIdPhotoFrontPath(int memberId) {
        return registrationFileRepository
                .findFirstByMemberIdAndType(memberId, "id-front")
                .map(RegistrationFile::getS3Key);
    }

    public Optional<String> getIdPhotoBackPath(int memberId) {
        return registrationFileRepository
                .findFirstByMemberIdAndType(memberId, "id-back")
                .map(RegistrationFile::getS3Key);
    }
}
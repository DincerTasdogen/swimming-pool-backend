package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.RegistrationFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationFileRepository extends JpaRepository<RegistrationFile, Long> {
    Optional<RegistrationFile> findByS3Key(String s3Key);
    Optional<RegistrationFile> findFirstByMemberIdAndType(int memberId, String type);
    List<RegistrationFile> findAllByMemberId(int memberId);
}

package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.MemberPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberPackageRepository extends JpaRepository<MemberPackage, Integer> {
    List<MemberPackage> findByMemberIdAndActiveTrue(int memberId);
    List<MemberPackage> findByMemberIdAndActiveFalse(int memberId);
    boolean existsByMemberIdAndActiveTrue(int memberId);
    boolean existsByMemberIdAndActiveTrueAndPoolIdIsNull(int memberId);
    boolean existsByMemberIdAndActiveTrueAndPoolId(int memberId, int poolId);
}

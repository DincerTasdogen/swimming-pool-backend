package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberPackageRepository extends JpaRepository<MemberPackage, Integer> {
    List<MemberPackage> findByMemberIdAndActiveTrue(int memberId);
    List<MemberPackage> findByMemberIdAndActiveFalse(int memberId);
    List<MemberPackage> findByMemberId(int memberId);
    boolean existsByMemberIdAndActiveTrue(int memberId);
    boolean existsByMemberIdAndActiveTrueAndPoolIdIsNull(int memberId);
    boolean existsByMemberIdAndActiveTrueAndPoolId(int memberId, int poolId);
    List<MemberPackage> findByMemberIdAndActive(int memberId, boolean active);
    List<MemberPackage> findByMemberIdAndPaymentStatus(int memberId, MemberPackagePaymentStatusEnum paymentStatus);
    List<MemberPackage> findByMemberIdAndActiveAndPaymentStatus(
            int memberId,
            boolean active,
            MemberPackagePaymentStatusEnum paymentStatus);
    List<MemberPackage> findByMemberIdAndPackageTypeIdAndActive(
            int memberId,
            int packageTypeId,
            boolean active);

    boolean existsByPackageTypeIdAndActiveTrue(int id);

    List<MemberPackage> findByMemberIdAndActiveTrueAndPaymentStatus(int memberId, MemberPackagePaymentStatusEnum memberPackagePaymentStatusEnum);

    boolean existsByMemberIdAndActiveTrueAndPaymentStatus(int memberId, MemberPackagePaymentStatusEnum memberPackagePaymentStatusEnum);
}

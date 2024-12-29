package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.entity.MemberPackage;
import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPackageDTO {
    private int memberId;
    private int packageTypeId;
    private LocalDateTime purchaseDate;
    private int sessionsRemaining;
    private boolean active;  // Changed from isActive to active
    private int poolId;
    private MemberPackagePaymentStatusEnum paymentStatus;
    private LocalDateTime paymentDate;

    public static MemberPackage convertToMemberPackage(MemberPackageDTO dto) {
        if (dto == null) return null;

        MemberPackage memberPackage = new MemberPackage();
        memberPackage.setMemberId(dto.getMemberId());
        memberPackage.setPackageTypeId(dto.getPackageTypeId());
        memberPackage.setPurchaseDate(dto.getPurchaseDate());
        memberPackage.setSessionsRemaining(dto.getSessionsRemaining());
        memberPackage.setActive(dto.isActive());
        memberPackage.setPoolId(dto.getPoolId());
        memberPackage.setPaymentStatus(dto.getPaymentStatus());
        memberPackage.setPaymentDate(dto.getPaymentDate());

        return memberPackage;
    }

    public static MemberPackageDTO createFromMemberPackage(MemberPackage memberPackage) {
        if (memberPackage == null) return null;

        return MemberPackageDTO.builder()
                .memberId(memberPackage.getMemberId())
                .packageTypeId(memberPackage.getPackageTypeId())
                .purchaseDate(memberPackage.getPurchaseDate())
                .sessionsRemaining(memberPackage.getSessionsRemaining())
                .active(memberPackage.isActive())
                .poolId(memberPackage.getPoolId())
                .paymentStatus(memberPackage.getPaymentStatus())
                .paymentDate(memberPackage.getPaymentDate())
                .build();
    }
}
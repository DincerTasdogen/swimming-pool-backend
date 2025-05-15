package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime; // Needed for packageStartTime/EndTime

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPackageDTO {
    private int id;
    private int memberId;
    private int packageTypeId;
    private LocalDateTime purchaseDate;
    private int sessionsRemaining;
    private boolean active;
    private int poolId;
    private MemberPackagePaymentStatusEnum paymentStatus;
    private LocalDateTime paymentDate;

    private String packageName;
    private String packageDescription;
    private LocalTime packageStartTime;
    private LocalTime packageEndTime;
    private boolean isEducationPackage;
    private boolean requiresSwimmingAbility;
    private boolean multiplePools;
    private int packageSessionLimit;

    private String poolName;
    private String poolCity;
}

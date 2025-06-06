package com.sp.SwimmingPool.model.entity;

import com.sp.SwimmingPool.model.enums.MemberPackagePaymentStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_package",
        indexes = {
                @Index(name = "idx_member_package_member", columnList = "memberId"),
                @Index(name = "idx_memberpackage_member_active", columnList = "memberId,active"),
                @Index(name = "idx_memberpackage_member_status", columnList = "memberId,paymentStatus"),
                @Index(name = "idx_memberpackage_packagetype", columnList = "packageTypeId"),
                @Index(name = "idx_memberpackage_pool", columnList = "poolId")
        }
)
@Getter
@Setter
public class MemberPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private int memberId;

    @Column(nullable = false)
    private int packageTypeId;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime purchaseDate;

    @Column(columnDefinition = "int default 0")
    private int sessionsRemaining;

    @Column(columnDefinition = "boolean default true")
    private boolean active;

    private int poolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberPackagePaymentStatusEnum paymentStatus;

    private LocalDateTime paymentDate;

}

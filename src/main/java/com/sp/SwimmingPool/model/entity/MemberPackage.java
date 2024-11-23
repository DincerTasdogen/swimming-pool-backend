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
                @Index(name = "idx_member_package_member", columnList = "memberId")
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
    private int sessionsUsed;

    @Column(columnDefinition = "boolean default true")
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberPackagePaymentStatusEnum paymentStatus;

    private LocalDateTime paymentDate;

}

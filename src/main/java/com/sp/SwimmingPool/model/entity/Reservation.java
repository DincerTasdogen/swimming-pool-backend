package com.sp.SwimmingPool.model.entity;

import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservation",
        indexes = {
                @Index(name = "uk_member_session", columnList = "memberId,sessionId", unique = true),
                @Index(name = "idx_reservation_date", columnList = "createdAt"),
                @Index(name = "idx_reservation_member_status", columnList = "memberId,status"),
                @Index(name = "idx_reservation_status", columnList = "status")
        }
)
@Getter
@Setter
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private int memberId;

    @Column(nullable = false)
    private int sessionId;

    @Column(nullable = false)
    private int memberPackageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatusEnum status;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}

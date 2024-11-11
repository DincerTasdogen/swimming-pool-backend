package com.example.Graduation.Project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "health_info",
        indexes = {
                @Index(name = "uk_member_id", columnList = "memberId", unique = true),
                @Index(name = "idx_health_info_member", columnList = "memberId")
        }
)
@Getter
@Setter
public class HealthInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private int memberId;

    @Column(nullable = false)
    private boolean question1;

    @Column(nullable = false)
    private boolean question2;

    @Column(nullable = false)
    private boolean question3;

    @Column(nullable = false)
    private boolean question4;

    @Column(nullable = false)
    private boolean question5;

    @Column(nullable = false)
    private boolean question6;

    @Column(nullable = false)
    private boolean question7;

    private String medicalReportPath;

    @Column(columnDefinition = "boolean default false")
    private boolean doctorApproved;

    private String doctorNotes;

    private Integer doctorId;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // Getters and Setters
}

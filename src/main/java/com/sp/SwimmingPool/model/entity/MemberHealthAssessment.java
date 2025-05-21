package com.sp.SwimmingPool.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList; // Initialize list
import java.util.List;

@Entity
@Getter
@Setter
@Table(
        name = "member_health_assessment",
        indexes =
                {
                    @Index(name = "idx_memberhealthassessment_memberid", columnList = "memberId")
                }
)
public class MemberHealthAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int memberId;

    @OneToMany(
            mappedBy = "healthAssessment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<HealthAnswer> answers = new ArrayList<>();

    private double riskScore;

    @Column(columnDefinition = "boolean default false")
    private boolean requiresMedicalReport;

    @Column(columnDefinition = "boolean default false")
    private boolean doctorApproved;
    private String medicalReportPath;
    private String doctorNotes;
    private Integer doctorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void addAnswer(HealthAnswer answer) {
        this.answers.add(answer);
        answer.setHealthAssessment(this);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

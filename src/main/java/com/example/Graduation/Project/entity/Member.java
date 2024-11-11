package com.example.Graduation.Project.entity;

import com.example.Graduation.Project.enums.MemberGenderEnum;
import com.example.Graduation.Project.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "member",
        indexes = {
                @Index(name = "uk_email", columnList = "email", unique = true),
                @Index(name = "uk_identity_number", columnList = "identityNumber", unique = true),
                @Index(name = "idx_member_status", columnList = "status"),
                @Index(name = "idx_member_coach", columnList = "coachId")
        }
)
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 11)
    private String identityNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberGenderEnum gender;

    @Column(precision = 5)
    private Double weight;

    @Column(precision = 5)
    private Double height;

    @Column(nullable = false)
    private LocalDate birthDate;

    private String phoneNumber;

    @Column(nullable = false)
    private String idPhotoFront;

    @Column(nullable = false)
    private String idPhotoBack;

    @Column(nullable = false)
    private String photo;

    @Column(columnDefinition = "boolean default false")
    private boolean canSwim;

    private Integer coachId;

    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(columnDefinition = "boolean default false")
    private boolean photoVerified;

    @Column(columnDefinition = "boolean default false")
    private boolean idVerified;

    @Column(columnDefinition = "boolean default false")
    private boolean emailVerified;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime registrationDate;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}

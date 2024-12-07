package com.sp.SwimmingPool.model.entity;

import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
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


    @Builder
    public Member(
            String identityNumber,
            String email,
            String password,
            String name,
            String surname,
            MemberGenderEnum gender,
            Double weight,
            Double height,
            LocalDate birthDate,
            String phoneNumber,
            String idPhotoFront,
            String idPhotoBack,
            String photo,
            boolean canSwim,
            Integer coachId,
            StatusEnum status,
            boolean photoVerified,
            boolean idVerified,
            LocalDateTime registrationDate,
            LocalDateTime updatedAt
    ) {
        this.identityNumber = identityNumber;
        this.email = email;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.gender = gender;
        this.weight = weight;
        this.height = height;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.idPhotoFront = idPhotoFront;
        this.idPhotoBack = idPhotoBack;
        this.photo = photo;
        this.canSwim = canSwim;
        this.coachId = coachId;
        this.status = status;
        this.photoVerified = photoVerified;
        this.idVerified = idVerified;
        this.registrationDate = registrationDate;
        this.updatedAt = updatedAt;
    }
}

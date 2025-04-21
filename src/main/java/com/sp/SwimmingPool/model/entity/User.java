package com.sp.SwimmingPool.model.entity;

import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user",
        indexes = {
                @Index(name = "uk_email", columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

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
    private UserRoleEnum role;

    @Column(columnDefinition = "int default 0")
    private int memberCount;

    @Column
    private String passwordResetToken;

    @Column
    private LocalDateTime tokenExpiryDate;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Builder
    public User(
            String email,
            String password,
            String name,
            String surname,
            UserRoleEnum role,
            int memberCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.role = role;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

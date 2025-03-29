package com.sp.SwimmingPool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO {
    private int id;
    private String name;
    private String surname;
    private String email;
    private String identityNumber;
    private String gender;
    private Double weight;
    private Double height;
    private LocalDate birthDate;
    private String phoneNumber;
    private String idPhotoFront;
    private String idPhotoBack;
    private String photo;
    private boolean canSwim;
    private String swimmingLevel;
    private LocalDateTime lastLessonDate;
    private String swimmingNotes;
    private Integer coachId;
}
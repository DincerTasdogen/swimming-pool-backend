package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String surname;

    @NotBlank
    private String identityNumber;

    private String phoneNumber;
    private LocalDate birthDate;
    private MemberGenderEnum gender;
}

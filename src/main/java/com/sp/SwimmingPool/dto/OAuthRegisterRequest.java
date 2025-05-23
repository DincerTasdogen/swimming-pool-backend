package com.sp.SwimmingPool.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthRegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Surname is required")
    @Size(min = 2, max = 50, message = "Surname must be between 2 and 50 characters")
    private String surname;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\+90\\d{10}", message = "Phone number must start with +90 followed by 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Identity number is required")
    @Pattern(regexp = "\\d{11}", message = "Identity number must be 11 digits")
    private String identityNumber;

    @NotNull(message = "Birth date is required")
    private LocalDate birthDate;

    @NotNull(message = "Height is required")
    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 250, message = "Height must be at most 250 cm")
    private Double height;

    @NotNull(message = "Weight is required")
    @Min(value = 20, message = "Weight must be at least 20 kg")
    @Max(value = 200, message = "Weight must be at most 200 kg")
    private Double weight;

    private String doctorToldCondition;
    private String chestPain;
    private String dizziness;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE or OTHER")
    private String gender;

    private boolean canSwim;
    private String photo;
    private String idPhotoFront;
    private String idPhotoBack;
}
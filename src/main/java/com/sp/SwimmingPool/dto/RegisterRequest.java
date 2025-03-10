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
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Surname is required")
    @Size(min = 2, max = 50, message = "Surname must be between 2 and 50 characters")
    private String surname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "\\+90\\d{10}", message = "Phone number must start with +90 and contain 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Identity number is required")
    @Pattern(regexp = "\\d{11}", message = "Identity number must be 11 digits")
    private String identityNumber;

    @NotNull(message = "Birth date is required")
    private LocalDate birthDate;

    @NotNull(message = "Height is required")
    @Min(value = 50, message = "Height must be at least 50 cm")
    private Double height;

    @NotNull(message = "Weight is required")
    @Min(value = 20, message = "Weight must be at least 20 kg")
    private Double weight;

    @NotBlank(message = "Gender is required")
    private String gender;

    private boolean canSwim;

    private String doctorToldCondition;
    private String chestPain;
    private String dizziness;

    // Document paths stored by the storage service
    private String photo;
    private String idPhotoFront;
    private String idPhotoBack;
}
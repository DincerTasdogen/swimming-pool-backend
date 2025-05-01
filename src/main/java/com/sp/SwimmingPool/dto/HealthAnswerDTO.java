package com.sp.SwimmingPool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HealthAnswerDTO {
    private String questionText;
    private boolean answer;
    private String notes;
}
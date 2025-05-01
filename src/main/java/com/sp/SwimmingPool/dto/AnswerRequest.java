package com.sp.SwimmingPool.dto;

import lombok.Data;

@Data
public class AnswerRequest {
    private Long questionId;
    private boolean answer;
    private String notes;
}
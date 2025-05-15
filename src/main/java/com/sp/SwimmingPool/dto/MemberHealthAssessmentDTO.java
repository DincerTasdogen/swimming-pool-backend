package com.sp.SwimmingPool.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Data
public class MemberHealthAssessmentDTO {
    private double riskScore;
    private String riskLevel;
    private String riskLevelDescription;
    private boolean requiresMedicalReport;
    private List<HealthAnswerDTO> answers;
}

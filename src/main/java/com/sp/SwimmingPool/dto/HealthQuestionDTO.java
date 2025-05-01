package com.sp.SwimmingPool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthQuestionDTO {
    private Long id;
    private String questionText;
}

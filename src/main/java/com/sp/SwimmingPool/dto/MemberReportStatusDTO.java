package com.sp.SwimmingPool.dto;

import com.sp.SwimmingPool.model.enums.StatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberReportStatusDTO {
    private StatusEnum currentMemberStatus;
    private String doctorNotes;
    private boolean requiresMedicalReport;
    private String medicalReportPath;
    private LocalDateTime reportUpdatedAt;
    private boolean doctorApproved;
}
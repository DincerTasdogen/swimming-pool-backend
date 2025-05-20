package com.sp.SwimmingPool.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DoctorReviewRequest {

    @NotNull(message = "Eligibility status must be provided.")
    private Boolean eligibleForPool;

    @NotNull(message = "Document validity status must be provided.")
    private Boolean documentInvalid;

    @Size(max = 1000, message = "Doctor notes cannot exceed 1000 characters.")
    private String doctorNotes;

    @AssertTrue(message = "Doctor notes are required when rejecting the report or flagging the document as invalid.")
    private boolean isNotesValid() {
        // If document is invalid OR member is not eligible, then notes must be present.
        if ((documentInvalid != null && documentInvalid) || (eligibleForPool != null && !eligibleForPool)) {
            return doctorNotes != null && !doctorNotes.trim().isEmpty();
        }
        // Otherwise, notes are optional.
        return true;
    }
}
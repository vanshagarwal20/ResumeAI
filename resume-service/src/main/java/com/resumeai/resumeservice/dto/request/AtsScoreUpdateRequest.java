package com.resumeai.resumeservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * AtsScoreUpdateRequest — payload for updating the ATS score.
 *
 * Called by ai-service (internal) after completing an ATS compatibility check.
 * The score is a number from 0 to 100.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsScoreUpdateRequest {

    /**
     * The computed ATS compatibility score.
     * 0 = completely incompatible with the job description
     * 100 = perfect keyword and structure match
     */
    @NotNull(message = "ATS score is required")
    @Min(value = 0, message = "ATS score must be at least 0")
    @Max(value = 100, message = "ATS score must not exceed 100")
    private Integer atsScore;
}


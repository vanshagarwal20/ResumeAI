package com.resumeai.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiSummaryRequest {

    @NotBlank(message = "jobTitle is required")
    @Size(max = 200)
    private String jobTitle;

    @NotBlank(message = "skills is required")
    @Size(max = 1000)
    private String skills;

    @NotBlank(message = "experienceLevel is required")
    @Size(max = 100)
    private String experienceLevel;
}

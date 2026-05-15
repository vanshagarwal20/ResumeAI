package com.resumeai.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiExperienceRequest {

    @NotBlank(message = "jobTitle is required")
    @Size(max = 200)
    private String jobTitle;

    // companyName is optional — frontend may not have it yet
    @Size(max = 200)
    private String companyName;

    @NotBlank(message = "workSummary is required")
    @Size(max = 2000)
    private String workSummary;
}
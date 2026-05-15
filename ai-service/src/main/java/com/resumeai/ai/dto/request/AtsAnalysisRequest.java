package com.resumeai.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtsAnalysisRequest {

    private Integer resumeId;

    @NotBlank(message = "resumeText is required")
    @Size(max = 20000)
    private String resumeText;

    @NotBlank(message = "jobDescription is required")
    @Size(max = 20000)
    private String jobDescription;
}


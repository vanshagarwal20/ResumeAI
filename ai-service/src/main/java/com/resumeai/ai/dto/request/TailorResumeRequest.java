package com.resumeai.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TailorResumeRequest {

    @NotBlank(message = "resumeJson is required")
    @Size(max = 60000)
    private String resumeJson;

    @NotBlank(message = "jobDescription is required")
    @Size(max = 30000)
    private String jobDescription;
}

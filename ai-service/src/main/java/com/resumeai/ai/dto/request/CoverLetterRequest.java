package com.resumeai.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CoverLetterRequest {

    @NotBlank(message = "resumeText is required")
    @Size(max = 30000)
    private String resumeText;

    @NotBlank(message = "jobDescription is required")
    @Size(max = 30000)
    private String jobDescription;

    @Size(max = 200)
    private String companyName;
}

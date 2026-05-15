package com.resumeai.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TranslateResumeRequest {

    @NotBlank(message = "resumeJson is required")
    @Size(max = 60000)
    private String resumeJson;

    @NotBlank(message = "targetLanguage is required")
    @Size(max = 80)
    private String targetLanguage;
}

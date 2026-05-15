package com.resumeai.jobmatch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnalyzeJobFitRequest {

    @NotNull(message = "resumeId is required")
    private Integer resumeId;

    @NotBlank(message = "jobTitle is required")
    @Size(max = 200)
    private String jobTitle;

    @Size(max = 200)
    private String companyName;

    @Size(max = 1000)
    private String jobUrl;

    @NotBlank(message = "jobDescription is required")
    @Size(max = 20000)
    private String jobDescription;
}

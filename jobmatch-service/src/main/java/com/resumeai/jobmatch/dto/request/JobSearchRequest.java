package com.resumeai.jobmatch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobSearchRequest {

    @NotBlank(message = "jobTitle is required")
    @Size(max = 200)
    private String jobTitle;

    @Size(max = 200)
    private String location;
}

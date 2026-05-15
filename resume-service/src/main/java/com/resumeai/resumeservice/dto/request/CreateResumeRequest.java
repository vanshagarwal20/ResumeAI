package com.resumeai.resumeservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * CreateResumeRequest — payload for creating a new resume.
 *
 * The user selects a template and provides a title and target job title.
 * The rest (sections, ATS score, etc.) are populated later during editing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateResumeRequest {

    /**
     * Dashboard display label for this resume.
     * e.g., "My Software Engineer Resume" or "Resume for Google Application"
     */
    @NotBlank(message = "Resume title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    /**
     * The job role this resume targets.
     * Used by AI for tailored content generation.
     */
    @Size(max = 200, message = "Target job title must not exceed 200 characters")
    private String targetJobTitle;

    /**
     * ID of the template to use for this resume.
     * Must be a valid template in template-service.
     * If null, a default free template will be applied.
     */
    private Integer templateId;
}


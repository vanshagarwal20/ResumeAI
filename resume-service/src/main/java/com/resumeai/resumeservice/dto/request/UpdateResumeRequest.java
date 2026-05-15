package com.resumeai.resumeservice.dto.request;
import com.resumeai.resumeservice.entity.Resume.ResumeStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * UpdateResumeRequest — payload for updating resume metadata.
 * All fields are optional (PATCH semantics — only non-null fields are updated).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateResumeRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 200, message = "Target job title must not exceed 200 characters")
    private String targetJobTitle;

    /** Change the template — null means keep current */
    private Integer templateId;

    /** Manually set status to COMPLETE when ready to apply */
    private ResumeStatus status;

    /** ISO 639-1 language code (e.g., "en", "hi", "fr") */
    @Size(max = 10)
    private String language;
}


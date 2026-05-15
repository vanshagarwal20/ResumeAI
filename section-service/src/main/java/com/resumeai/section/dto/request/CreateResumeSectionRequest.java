package com.resumeai.section.dto.request;

import com.resumeai.section.entity.ResumeSection.SectionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateResumeSectionRequest {

    @NotNull(message = "resumeId is required")
    private Integer resumeId;

    @NotNull(message = "sectionType is required")
    private SectionType sectionType;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;

    @Min(value = 1, message = "displayOrder must be at least 1")
    private Integer displayOrder;

    private Boolean isVisible;
}

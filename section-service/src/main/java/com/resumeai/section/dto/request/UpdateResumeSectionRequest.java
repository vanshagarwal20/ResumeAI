package com.resumeai.section.dto.request;

import com.resumeai.section.entity.ResumeSection.SectionType;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateResumeSectionRequest {

    private SectionType sectionType;
    private String title;
    private String content;

    @Min(value = 1, message = "displayOrder must be at least 1")
    private Integer displayOrder;

    private Boolean isVisible;
}

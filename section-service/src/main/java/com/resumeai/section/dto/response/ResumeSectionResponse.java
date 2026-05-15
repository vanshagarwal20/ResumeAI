package com.resumeai.section.dto.response;

import com.resumeai.section.entity.ResumeSection.SectionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResumeSectionResponse {

    private Integer sectionId;
    private Integer resumeId;
    private Integer userId;
    private SectionType sectionType;
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

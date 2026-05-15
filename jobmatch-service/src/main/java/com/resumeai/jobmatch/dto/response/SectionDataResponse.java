package com.resumeai.jobmatch.dto.response;

import lombok.Data;

@Data
public class SectionDataResponse {
    private Integer sectionId;
    private Integer resumeId;
    private Integer userId;
    private String sectionType;
    private String title;
    private String content;
    private Integer displayOrder;
    private Boolean isVisible;
}


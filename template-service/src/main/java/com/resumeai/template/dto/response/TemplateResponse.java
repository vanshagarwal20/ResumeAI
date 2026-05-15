package com.resumeai.template.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TemplateResponse {
    private Integer templateId;
    private String name;
    private String category;
    private String description;
    private String previewImageUrl;
    private String htmlStructure;
    private Boolean isPremium;
    private Boolean isPublic;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


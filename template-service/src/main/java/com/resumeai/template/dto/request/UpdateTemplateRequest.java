package com.resumeai.template.dto.request;

import lombok.Data;

@Data
public class UpdateTemplateRequest {
    private String name;
    private String category;
    private String description;
    private String previewImageUrl;
    private String htmlStructure;
    private Boolean isPremium;
    private Boolean isPublic;
    private Boolean isActive;
}


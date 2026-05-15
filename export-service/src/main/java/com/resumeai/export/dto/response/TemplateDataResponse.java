package com.resumeai.export.dto.response;

import lombok.Data;

@Data
public class TemplateDataResponse {
    private Integer templateId;
    private String name;
    private String category;
    private String description;
    private String previewImageUrl;
    private String htmlStructure;
    private Boolean isPremium;
    private Boolean isPublic;
    private Boolean isActive;
}

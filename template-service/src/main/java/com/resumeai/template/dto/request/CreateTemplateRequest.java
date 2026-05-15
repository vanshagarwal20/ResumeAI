package com.resumeai.template.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTemplateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "category is required")
    @Size(max = 100)
    private String category;

    @NotBlank(message = "description is required")
    @Size(max = 500)
    private String description;

    @NotBlank(message = "previewImageUrl is required")
    @Size(max = 1000)
    private String previewImageUrl;

    @NotBlank(message = "htmlStructure is required")
    private String htmlStructure;

    @NotNull(message = "isPremium is required")
    private Boolean isPremium;

    private Boolean isPublic;
}


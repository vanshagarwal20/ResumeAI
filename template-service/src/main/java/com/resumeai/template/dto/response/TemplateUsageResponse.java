package com.resumeai.template.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TemplateUsageResponse {
    private Integer templateId;
    private Integer usageCount;
    private List<Object> resumes;
}


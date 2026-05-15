package com.resumeai.template.service;

import com.resumeai.template.dto.request.CreateTemplateRequest;
import com.resumeai.template.dto.request.UpdateTemplateRequest;
import com.resumeai.template.dto.response.TemplateResponse;
import com.resumeai.template.dto.response.TemplateUsageResponse;

import java.util.List;

public interface TemplateService {

    TemplateResponse createTemplate(CreateTemplateRequest request);

    List<TemplateResponse> getAllTemplates();

    List<TemplateResponse> getPublicTemplates();

    List<TemplateResponse> getPublicTemplates(String category, Boolean premium);

    TemplateResponse getTemplateById(Integer templateId);

    TemplateResponse updateTemplate(Integer templateId, UpdateTemplateRequest request);

    void deleteTemplate(Integer templateId);

    TemplateUsageResponse getTemplateUsage(Integer templateId);
}

package com.resumeai.template.service.impl;

import com.resumeai.template.dto.request.CreateTemplateRequest;
import com.resumeai.template.dto.request.UpdateTemplateRequest;
import com.resumeai.template.dto.response.TemplateResponse;
import com.resumeai.template.dto.response.TemplateUsageResponse;
import com.resumeai.template.entity.ResumeTemplate;
import com.resumeai.template.exception.ResourceNotFoundException;
import com.resumeai.template.repository.ResumeTemplateRepository;
import com.resumeai.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final ResumeTemplateRepository templateRepository;
    private final RestTemplate restTemplate;

    @Value("${services.resume-service-url}")
    private String resumeServiceUrl;

    @Override
    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        ResumeTemplate template = ResumeTemplate.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .previewImageUrl(request.getPreviewImageUrl())
                .htmlStructure(request.getHtmlStructure())
                .isPremium(request.getIsPremium())
                .isPublic(request.getIsPublic() == null ? true : request.getIsPublic())
                .isActive(true)
                .build();

        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getPublicTemplates() {
        return templateRepository.findByIsPublicTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> getPublicTemplates(String category, Boolean premium) {
        return getPublicTemplates().stream()
                .filter(template -> category == null || category.isBlank()
                        || category.equalsIgnoreCase(template.getCategory()))
                .filter(template -> premium == null || premium.equals(template.getIsPremium()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(Integer templateId) {
        return mapToResponse(findTemplate(templateId));
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(Integer templateId, UpdateTemplateRequest request) {
        ResumeTemplate template = findTemplate(templateId);

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getPreviewImageUrl() != null) {
            template.setPreviewImageUrl(request.getPreviewImageUrl());
        }
        if (request.getHtmlStructure() != null) {
            template.setHtmlStructure(request.getHtmlStructure());
        }
        if (request.getIsPremium() != null) {
            template.setIsPremium(request.getIsPremium());
        }
        if (request.getIsPublic() != null) {
            template.setIsPublic(request.getIsPublic());
        }
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }

        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(Integer templateId) {
        ResumeTemplate template = findTemplate(templateId);
        template.setIsActive(false);
        templateRepository.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateUsageResponse getTemplateUsage(Integer templateId) {
        findTemplate(templateId);

        String url = resumeServiceUrl + "/api/v1/resumes/template/" + templateId;

        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        List<Object> resumes = response != null && response.get("data") instanceof List<?> list
                ? (List<Object>) list
                : Collections.emptyList();

        return TemplateUsageResponse.builder()
                .templateId(templateId)
                .usageCount(resumes.size())
                .resumes(resumes)
                .build();
    }

    private ResumeTemplate findTemplate(Integer templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ResumeTemplate", "templateId", templateId));
    }

    private TemplateResponse mapToResponse(ResumeTemplate template) {
        return TemplateResponse.builder()
                .templateId(template.getTemplateId())
                .name(template.getName())
                .category(template.getCategory())
                .description(template.getDescription())
                .previewImageUrl(template.getPreviewImageUrl())
                .htmlStructure(template.getHtmlStructure())
                .isPremium(template.getIsPremium())
                .isPublic(template.getIsPublic())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}


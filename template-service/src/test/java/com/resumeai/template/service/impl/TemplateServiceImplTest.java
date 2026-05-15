package com.resumeai.template.service.impl;

import com.resumeai.template.dto.request.CreateTemplateRequest;
import com.resumeai.template.dto.request.UpdateTemplateRequest;
import com.resumeai.template.dto.response.TemplateResponse;
import com.resumeai.template.entity.ResumeTemplate;
import com.resumeai.template.exception.ResourceNotFoundException;
import com.resumeai.template.repository.ResumeTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private ResumeTemplateRepository templateRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private ResumeTemplate sampleTemplate;

    @BeforeEach
    void setUp() {
        sampleTemplate = ResumeTemplate.builder()
                .templateId(1)
                .name("Modern Template")
                .category("Professional")
                .description("A modern resume template")
                .previewImageUrl("http://example.com/preview.png")
                .htmlStructure("<html></html>")
                .isPremium(false)
                .isPublic(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createTemplate — should save and return template")
    void createTemplate_shouldSave() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Modern Template");
        request.setCategory("Professional");
        request.setDescription("A modern resume template");
        request.setPreviewImageUrl("http://example.com/preview.png");
        request.setHtmlStructure("<html></html>");
        request.setIsPremium(false);

        when(templateRepository.save(any(ResumeTemplate.class))).thenReturn(sampleTemplate);

        TemplateResponse result = templateService.createTemplate(request);

        assertThat(result).isNotNull();
        assertThat(result.getTemplateId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("Modern Template");
        verify(templateRepository).save(any(ResumeTemplate.class));
    }

    @Test
    @DisplayName("getAllTemplates — should return active templates")
    void getAllTemplates_shouldReturnActiveTemplates() {
        when(templateRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(sampleTemplate));

        List<TemplateResponse> result = templateService.getAllTemplates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Modern Template");
    }

    @Test
    @DisplayName("getPublicTemplates — should return public and active templates")
    void getPublicTemplates_shouldReturnPublicActiveTemplates() {
        when(templateRepository.findByIsPublicTrueAndIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(sampleTemplate));

        List<TemplateResponse> result = templateService.getPublicTemplates();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getTemplateById — should return template when found")
    void getTemplateById_shouldReturn() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(sampleTemplate));

        TemplateResponse result = templateService.getTemplateById(1);

        assertThat(result.getTemplateId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getTemplateById — should throw when not found")
    void getTemplateById_shouldThrowWhenNotFound() {
        when(templateRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.getTemplateById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateTemplate — should update non-null fields")
    void updateTemplate_shouldUpdateFields() {
        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Updated Template");
        request.setDescription("Updated description");

        when(templateRepository.findById(1)).thenReturn(Optional.of(sampleTemplate));
        when(templateRepository.save(any(ResumeTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateResponse result = templateService.updateTemplate(1, request);

        assertThat(result.getName()).isEqualTo("Updated Template");
        assertThat(result.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("deleteTemplate — should soft-delete by setting isActive=false")
    void deleteTemplate_shouldSoftDelete() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(sampleTemplate));
        when(templateRepository.save(any(ResumeTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        templateService.deleteTemplate(1);

        assertThat(sampleTemplate.getIsActive()).isFalse();
        verify(templateRepository).save(sampleTemplate);
    }

    @Test
    @DisplayName("getPublicTemplates with filters — should filter by category and premium")
    void getPublicTemplates_withFilters() {
        ResumeTemplate premiumTemplate = ResumeTemplate.builder()
                .templateId(2).name("Premium Template").category("Creative")
                .description("desc").previewImageUrl("url").htmlStructure("<html/>")
                .isPremium(true).isPublic(true).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(templateRepository.findByIsPublicTrueAndIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(sampleTemplate, premiumTemplate));

        List<TemplateResponse> result = templateService.getPublicTemplates("Professional", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Professional");
    }
}

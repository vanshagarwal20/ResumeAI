package com.resumeai.export.service.impl;

import com.resumeai.export.dto.request.ExportResumeRequest;
import com.resumeai.export.dto.response.ExportHistoryResponse;
import com.resumeai.export.dto.response.ExportJobResponse;
import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.entity.ExportJob.ExportFormat;
import com.resumeai.export.entity.ExportJob.ExportStatus;
import com.resumeai.export.exception.ResourceNotFoundException;
import com.resumeai.export.repository.ExportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private ExportJobRepository exportJobRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExportServiceImpl exportService;

    private static final Integer USER_ID = 1;
    private ExportJob sampleExportJob;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportService, "resumeServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(exportService, "sectionServiceUrl", "http://localhost:8084");
        ReflectionTestUtils.setField(exportService, "templateServiceUrl", "http://localhost:8085");

        sampleExportJob = ExportJob.builder()
                .exportId(1)
                .userId(USER_ID)
                .resumeId(10)
                .templateId(1)
                .exportFormat(ExportFormat.PDF)
                .status(ExportStatus.COMPLETED)
                .fileName("Software_Engineer_Resume.pdf")
                .downloadUrl("http://localhost:8086/downloads/Software_Engineer_Resume.pdf")
                .generatedHtml("<html><body>Resume content</body></html>")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("exportResume — should fetch data, build HTML, and save export job")
    void exportResume_shouldSaveAndReturn() {
        ExportResumeRequest request = new ExportResumeRequest();
        request.setResumeId(10);
        request.setTemplateId(1);
        request.setExportFormat(ExportFormat.PDF);

        // Mock resume-service response
        Map<String, Object> resumeData = Map.of(
                "resumeId", 10, "userId", 1, "title", "Software Engineer Resume",
                "targetJobTitle", "Software Engineer", "templateId", 1,
                "atsScore", 75, "status", "DRAFT", "language", "en",
                "isPublic", false, "viewCount", 0
        );
        Map<String, Object> resumeResponse = Map.of("success", true, "data", resumeData);

        // Mock section-service response
        Map<String, Object> sectionData = Map.of(
                "sectionId", 1, "resumeId", 10, "userId", 1,
                "sectionType", "EXPERIENCE", "title", "Work Experience",
                "content", "Worked at XYZ Corp", "displayOrder", 1, "isVisible", true
        );
        Map<String, Object> sectionResponse = Map.of("success", true, "data", List.of(sectionData));

        // Mock template-service response
        Map<String, Object> templateData = Map.of(
                "templateId", 1, "name", "Modern", "category", "Professional",
                "description", "Modern template", "previewImageUrl", "url",
                "htmlStructure", "<html></html>", "isPremium", false,
                "isPublic", true, "isActive", true
        );
        Map<String, Object> templateResponse = Map.of("success", true, "data", templateData);

        when(restTemplate.exchange(contains("/api/v1/resumes/"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(resumeResponse));

        when(restTemplate.exchange(contains("/api/v1/sections/"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(sectionResponse));

        when(restTemplate.exchange(contains("/api/v1/templates/"), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(templateResponse));

        when(exportJobRepository.save(any(ExportJob.class))).thenReturn(sampleExportJob);

        ExportJobResponse result = exportService.exportResume(USER_ID, request, "Bearer test-token");

        assertThat(result).isNotNull();
        assertThat(result.getExportId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(ExportStatus.COMPLETED);
        verify(exportJobRepository).save(any(ExportJob.class));
    }

    @Test
    @DisplayName("getExportHistory — should return list of history responses")
    void getExportHistory_shouldReturnList() {
        ExportJob second = ExportJob.builder()
                .exportId(2).userId(USER_ID).resumeId(11).templateId(2)
                .exportFormat(ExportFormat.JSON).status(ExportStatus.COMPLETED)
                .fileName("Resume.json").downloadUrl("url")
                .createdAt(LocalDateTime.now()).build();

        when(exportJobRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(sampleExportJob, second));

        List<ExportHistoryResponse> result = exportService.getExportHistory(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getExportFormat()).isEqualTo(ExportFormat.PDF);
        assertThat(result.get(1).getExportFormat()).isEqualTo(ExportFormat.JSON);
    }

    @Test
    @DisplayName("getExportHistoryByResume — should return history for specific resume")
    void getExportHistoryByResume_shouldReturnFiltered() {
        when(exportJobRepository.findByUserIdAndResumeIdOrderByCreatedAtDesc(USER_ID, 10))
                .thenReturn(List.of(sampleExportJob));

        List<ExportHistoryResponse> result = exportService.getExportHistoryByResume(USER_ID, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResumeId()).isEqualTo(10);
    }

    @Test
    @DisplayName("getExportById — should return export when owned")
    void getExportById_shouldReturnWhenOwned() {
        when(exportJobRepository.findById(1)).thenReturn(Optional.of(sampleExportJob));

        ExportJobResponse result = exportService.getExportById(1, USER_ID);

        assertThat(result.getExportId()).isEqualTo(1);
        assertThat(result.getGeneratedHtml()).isNotNull();
    }

    @Test
    @DisplayName("getExportById — should throw when not found")
    void getExportById_shouldThrowWhenNotFound() {
        when(exportJobRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportService.getExportById(999, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getExportById — should throw when not owned by user")
    void getExportById_shouldThrowWhenNotOwned() {
        ExportJob otherUserJob = ExportJob.builder()
                .exportId(1).userId(999).resumeId(10).templateId(1)
                .exportFormat(ExportFormat.PDF).status(ExportStatus.COMPLETED)
                .fileName("file.pdf").downloadUrl("url")
                .createdAt(LocalDateTime.now()).build();

        when(exportJobRepository.findById(1)).thenReturn(Optional.of(otherUserJob));

        assertThatThrownBy(() -> exportService.getExportById(1, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

package com.resumeai.resumeservice.service.impl;

import com.resumeai.resumeservice.dto.request.AtsScoreUpdateRequest;
import com.resumeai.resumeservice.dto.request.CreateResumeRequest;
import com.resumeai.resumeservice.dto.request.UpdateResumeRequest;
import com.resumeai.resumeservice.dto.response.ResumeResponse;
import com.resumeai.resumeservice.entity.Resume;
import com.resumeai.resumeservice.entity.Resume.ResumeStatus;
import com.resumeai.resumeservice.exception.AccessDeniedException;
import com.resumeai.resumeservice.exception.ResourceNotFoundException;
import com.resumeai.resumeservice.exception.ResumeQuotaExceededException;
import com.resumeai.resumeservice.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private static final Integer USER_ID = 1;
    private Resume sampleResume;
    private ResumeResponse sampleResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resumeService, "freeTierMaxResumes", 3);
        ReflectionTestUtils.setField(resumeService, "sectionServiceUrl", "http://localhost:8084");

        sampleResume = Resume.builder()
                .resumeId(1)
                .userId(USER_ID)
                .title("Software Engineer Resume")
                .targetJobTitle("Software Engineer")
                .templateId(1)
                .status(ResumeStatus.DRAFT)
                .atsScore(0)
                .isPublic(false)
                .viewCount(0)
                .language("en")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleResponse = new ResumeResponse();
        sampleResponse.setResumeId(1);
        sampleResponse.setUserId(USER_ID);
        sampleResponse.setTitle("Software Engineer Resume");
        sampleResponse.setTargetJobTitle("Software Engineer");
        sampleResponse.setTemplateId(1);
        sampleResponse.setStatus(ResumeStatus.DRAFT);
        sampleResponse.setAtsScore(0);
        sampleResponse.setIsPublic(false);
        sampleResponse.setViewCount(0);
        sampleResponse.setLanguage("en");
    }

    // ── Create Resume ──────────────────────────────────────────

    @Test
    @DisplayName("createResume — FREE user under limit should succeed")
    void createResume_freeUserUnderLimit() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("Software Engineer Resume");
        request.setTargetJobTitle("Software Engineer");
        request.setTemplateId(1);

        when(resumeRepository.countByUserId(USER_ID)).thenReturn(2L);
        when(resumeRepository.save(any(Resume.class))).thenReturn(sampleResume);
        when(modelMapper.map(sampleResume, ResumeResponse.class)).thenReturn(sampleResponse);

        ResumeResponse result = resumeService.createResume(USER_ID, "FREE", request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Software Engineer Resume");
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    @DisplayName("createResume — FREE user at limit should throw quota exception")
    void createResume_freeUserAtLimit() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("Resume");
        request.setTargetJobTitle("Dev");
        request.setTemplateId(1);

        when(resumeRepository.countByUserId(USER_ID)).thenReturn(3L);

        assertThatThrownBy(() -> resumeService.createResume(USER_ID, "FREE", request))
                .isInstanceOf(ResumeQuotaExceededException.class);
    }

    @Test
    @DisplayName("createResume — PREMIUM user should bypass quota")
    void createResume_premiumUserNoLimit() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("Resume");
        request.setTargetJobTitle("Dev");
        request.setTemplateId(1);

        when(resumeRepository.save(any(Resume.class))).thenReturn(sampleResume);
        when(modelMapper.map(sampleResume, ResumeResponse.class)).thenReturn(sampleResponse);

        ResumeResponse result = resumeService.createResume(USER_ID, "PREMIUM", request);

        assertThat(result).isNotNull();
        verify(resumeRepository, never()).countByUserId(anyInt());
    }

    // ── Get Resume ─────────────────────────────────────────────

    @Test
    @DisplayName("getResumeById — should return when owned")
    void getResumeById_shouldReturnWhenOwned() {
        when(resumeRepository.findByResumeIdAndUserId(1, USER_ID)).thenReturn(Optional.of(sampleResume));
        when(modelMapper.map(sampleResume, ResumeResponse.class)).thenReturn(sampleResponse);

        ResumeResponse result = resumeService.getResumeById(1, USER_ID);

        assertThat(result.getResumeId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getResumeById — should throw when not found")
    void getResumeById_shouldThrowWhenNotFound() {
        when(resumeRepository.findByResumeIdAndUserId(999, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getResumeById(999, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getResumesByUser — should return list")
    void getResumesByUser_shouldReturnList() {
        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(sampleResume));
        when(modelMapper.map(sampleResume, ResumeResponse.class)).thenReturn(sampleResponse);

        List<ResumeResponse> result = resumeService.getResumesByUser(USER_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getMostRecentResume — should return most recent")
    void getMostRecentResume_shouldReturn() {
        when(resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.of(sampleResume));
        when(modelMapper.map(sampleResume, ResumeResponse.class)).thenReturn(sampleResponse);

        ResumeResponse result = resumeService.getMostRecentResume(USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getResumeId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMostRecentResume — should throw when user has no resumes")
    void getMostRecentResume_shouldThrowWhenEmpty() {
        when(resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getMostRecentResume(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Update Resume ──────────────────────────────────────────

    @Test
    @DisplayName("updateResume — should update non-null fields")
    void updateResume_shouldUpdateFields() {
        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setTitle("Updated Title");
        request.setTargetJobTitle("Senior Engineer");

        when(resumeRepository.findById(1)).thenReturn(Optional.of(sampleResume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(sampleResume);
        when(modelMapper.map(any(Resume.class), eq(ResumeResponse.class))).thenReturn(sampleResponse);

        ResumeResponse result = resumeService.updateResume(1, USER_ID, request);

        assertThat(result).isNotNull();
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    @DisplayName("updateResume — should throw when not owned")
    void updateResume_shouldThrowWhenNotOwned() {
        Resume otherUserResume = Resume.builder()
                .resumeId(1).userId(999).title("Other").build();

        when(resumeRepository.findById(1)).thenReturn(Optional.of(otherUserResume));

        assertThatThrownBy(() -> resumeService.updateResume(1, USER_ID, new UpdateResumeRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── Delete Resume ──────────────────────────────────────────

    @Test
    @DisplayName("deleteResume — should delete owned resume and its sections")
    void deleteResume_shouldDeleteOwnedResume() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(sampleResume));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        resumeService.deleteResume(1, USER_ID, "Bearer test-token");

        verify(resumeRepository).delete(sampleResume);
        verify(restTemplate).exchange(contains("/api/v1/sections/resume/1/all"),
                eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    // ── Duplicate Resume ───────────────────────────────────────

    @Test
    @DisplayName("duplicateResume — should duplicate and copy sections")
    void duplicateResume_shouldDuplicate() {
        Resume duplicated = Resume.builder()
                .resumeId(2).userId(USER_ID).title("Software Engineer Resume (Copy)")
                .targetJobTitle("Software Engineer").templateId(1)
                .status(ResumeStatus.DRAFT).atsScore(0).isPublic(false)
                .viewCount(0).language("en").build();

        ResumeResponse duplicatedResponse = new ResumeResponse();
        duplicatedResponse.setResumeId(2);
        duplicatedResponse.setTitle("Software Engineer Resume (Copy)");

        when(resumeRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(resumeRepository.findById(1)).thenReturn(Optional.of(sampleResume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(duplicated);
        when(modelMapper.map(duplicated, ResumeResponse.class)).thenReturn(duplicatedResponse);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        ResumeResponse result = resumeService.duplicateResume(1, USER_ID, "FREE", "Bearer token");

        assertThat(result.getResumeId()).isEqualTo(2);
        assertThat(result.getTitle()).contains("Copy");
        verify(restTemplate).exchange(contains("/duplicate/"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Void.class));
    }

    // ── ATS Score ──────────────────────────────────────────────

    @Test
    @DisplayName("updateAtsScore — should update score")
    void updateAtsScore_shouldUpdate() {
        AtsScoreUpdateRequest request = new AtsScoreUpdateRequest();
        request.setAtsScore(85);

        when(resumeRepository.existsById(1)).thenReturn(true);

        resumeService.updateAtsScore(1, request);

        verify(resumeRepository).updateAtsScore(1, 85);
    }

    @Test
    @DisplayName("updateAtsScore — should throw when not found")
    void updateAtsScore_shouldThrowWhenNotFound() {
        AtsScoreUpdateRequest request = new AtsScoreUpdateRequest();
        request.setAtsScore(85);

        when(resumeRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> resumeService.updateAtsScore(999, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Publish / Unpublish ────────────────────────────────────

    @Test
    @DisplayName("publishResume — should set isPublic to true")
    void publishResume_shouldPublish() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(sampleResume));

        resumeService.publishResume(1, USER_ID);

        verify(resumeRepository).updateIsPublic(1, true);
    }

    @Test
    @DisplayName("unpublishResume — should set isPublic to false")
    void unpublishResume_shouldUnpublish() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(sampleResume));

        resumeService.unpublishResume(1, USER_ID);

        verify(resumeRepository).updateIsPublic(1, false);
    }

    // ── Public Resumes ─────────────────────────────────────────

    @Test
    @DisplayName("getPublicResumes — should return public resumes sorted by views")
    void getPublicResumes_shouldReturnPublic() {
        Resume publicResume = Resume.builder()
                .resumeId(2).userId(2).title("Public Resume")
                .isPublic(true).viewCount(100).build();

        ResumeResponse publicResponse = new ResumeResponse();
        publicResponse.setResumeId(2);
        publicResponse.setIsPublic(true);

        when(resumeRepository.findByIsPublicOrderByViewCountDesc(true))
                .thenReturn(List.of(publicResume));
        when(modelMapper.map(publicResume, ResumeResponse.class)).thenReturn(publicResponse);

        List<ResumeResponse> result = resumeService.getPublicResumes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsPublic()).isTrue();
    }

    // ── View Count ─────────────────────────────────────────────

    @Test
    @DisplayName("incrementViewCount — should call repo when resume exists")
    void incrementViewCount_shouldCallRepo() {
        when(resumeRepository.existsById(1)).thenReturn(true);

        resumeService.incrementViewCount(1);

        verify(resumeRepository).incrementViewCount(1);
    }

    @Test
    @DisplayName("incrementViewCount — should throw when resume not found")
    void incrementViewCount_shouldThrowWhenNotFound() {
        when(resumeRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> resumeService.incrementViewCount(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Resumes by Template ────────────────────────────────────

    @Test
    @DisplayName("getResumesByTemplate — should return resumes using template")
    void getResumesByTemplate_shouldReturn() {
        when(resumeRepository.findByTemplateId(1)).thenReturn(List.of(sampleResume));
        when(modelMapper.map(sampleResume, ResumeResponse.class)).thenReturn(sampleResponse);

        List<ResumeResponse> result = resumeService.getResumesByTemplate(1);

        assertThat(result).hasSize(1);
    }
}

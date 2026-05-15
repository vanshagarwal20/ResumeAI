package com.resumeai.resumeservice.service.impl;

import com.resumeai.resumeservice.dto.request.AtsScoreUpdateRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.resumeai.resumeservice.dto.request.CreateResumeRequest;
import com.resumeai.resumeservice.dto.request.UpdateResumeRequest;
import com.resumeai.resumeservice.dto.response.ResumeResponse;
import com.resumeai.resumeservice.entity.Resume;
import com.resumeai.resumeservice.entity.Resume.ResumeStatus;
import com.resumeai.resumeservice.exception.AccessDeniedException;
import com.resumeai.resumeservice.exception.ResourceNotFoundException;
import com.resumeai.resumeservice.exception.ResumeQuotaExceededException;
import com.resumeai.resumeservice.repository.ResumeRepository;
import com.resumeai.resumeservice.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ResumeServiceImpl - implements resume lifecycle business logic.
 *
 * This version also integrates with section-service so:
 * - deleting a resume deletes all its sections
 * - duplicating a resume duplicates all its sections
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;

    @Value("${resume.free-tier-max-resumes:3}")
    private int freeTierMaxResumes;

    @Value("${services.section-service-url}")
    private String sectionServiceUrl;

    @Override
    @Transactional
    @CacheEvict(value = "resumeList", key = "#userId")
    public ResumeResponse createResume(Integer userId, String subscriptionPlan,
                                       CreateResumeRequest request) {
        log.info("Creating resume for user: {}, plan: {}", userId, subscriptionPlan);

        if ("FREE".equals(subscriptionPlan)) {
            long existingCount = resumeRepository.countByUserId(userId);
            if (existingCount >= freeTierMaxResumes) {
                throw new ResumeQuotaExceededException();
            }
        }

        Resume resume = Resume.builder()
                .userId(userId)
                .title(request.getTitle())
                .targetJobTitle(request.getTargetJobTitle())
                .templateId(request.getTemplateId())
                .status(ResumeStatus.DRAFT)
                .atsScore(0)
                .isPublic(false)
                .viewCount(0)
                .language("en")
                .build();

        Resume saved = resumeRepository.save(resume);
        log.info("Resume created with ID: {} for user: {}", saved.getResumeId(), userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "resumes", key = "#resumeId")  
    public ResumeResponse getResumeById(Integer resumeId, Integer userId) {
        Resume resume = resumeRepository.findByResumeIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume", "resumeId", resumeId));
        return mapToResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "resumeList", key = "#userId")
    public List<ResumeResponse> getResumesByUser(Integer userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getMostRecentResume(Integer userId) {
        Resume resume = resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "userId", userId));
        return mapToResponse(resume);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"resumes", "resumeList"}, allEntries = true)
    public ResumeResponse updateResume(Integer resumeId, Integer userId,
                                       UpdateResumeRequest request) {
        Resume resume = findOwnedResume(resumeId, userId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            resume.setTitle(request.getTitle());
        }
        if (request.getTargetJobTitle() != null) {
            resume.setTargetJobTitle(request.getTargetJobTitle());
        }
        if (request.getTemplateId() != null) {
            resume.setTemplateId(request.getTemplateId());
        }
        if (request.getStatus() != null) {
            resume.setStatus(request.getStatus());
        }
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            resume.setLanguage(request.getLanguage());
        }

        Resume updated = resumeRepository.save(resume);
        log.info("Resume {} updated by user {}", resumeId, userId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"resumes", "resumeList"}, allEntries = true) 
    public void deleteResume(Integer resumeId, Integer userId, String authorizationHeader) {
        Resume resume = findOwnedResume(resumeId, userId);

        // Delete child sections first so no orphan section data remains.
        deleteSectionsFromSectionService(resumeId, authorizationHeader);

        resumeRepository.delete(resume);
        log.info("Resume {} deleted by user {}", resumeId, userId);
    }

    @Override
    @Transactional
    public ResumeResponse duplicateResume(Integer resumeId, Integer userId,
                                          String subscriptionPlan, String authorizationHeader) {
        if ("FREE".equals(subscriptionPlan)) {
            long count = resumeRepository.countByUserId(userId);
            if (count >= freeTierMaxResumes) {
                throw new ResumeQuotaExceededException();
            }
        }

        Resume source = findOwnedResume(resumeId, userId);

        Resume duplicate = Resume.builder()
                .userId(userId)
                .title(source.getTitle() + " (Copy)")
                .targetJobTitle(source.getTargetJobTitle())
                .templateId(source.getTemplateId())
                .status(ResumeStatus.DRAFT)
                .atsScore(0)
                .isPublic(false)
                .viewCount(0)
                .language(source.getLanguage())
                .build();

        Resume saved = resumeRepository.save(duplicate);

        // Copy all sections from source resume to duplicated resume.
        duplicateSectionsInSectionService(source.getResumeId(), saved.getResumeId(), authorizationHeader);

        log.info("Resume {} duplicated to {} by user {}", resumeId, saved.getResumeId(), userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void updateAtsScore(Integer resumeId, AtsScoreUpdateRequest request) {
        if (!resumeRepository.existsById(resumeId)) {
            throw new ResourceNotFoundException("Resume", "resumeId", resumeId);
        }
        resumeRepository.updateAtsScore(resumeId, request.getAtsScore());
        log.info("ATS score updated to {} for resume {}", request.getAtsScore(), resumeId);
    }

    @Override
    @Transactional
    public void publishResume(Integer resumeId, Integer userId) {
        findOwnedResume(resumeId, userId);
        resumeRepository.updateIsPublic(resumeId, true);
        log.info("Resume {} published by user {}", resumeId, userId);
    }

    @Override
    @Transactional
    public void unpublishResume(Integer resumeId, Integer userId) {
        findOwnedResume(resumeId, userId);
        resumeRepository.updateIsPublic(resumeId, false);
        log.info("Resume {} unpublished by user {}", resumeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getPublicResumes() {
        return resumeRepository.findByIsPublicOrderByViewCountDesc(true)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementViewCount(Integer resumeId) {
        if (!resumeRepository.existsById(resumeId)) {
            throw new ResourceNotFoundException("Resume", "resumeId", resumeId);
        }
        resumeRepository.incrementViewCount(resumeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesByTemplate(Integer templateId) {
        return resumeRepository.findByTemplateId(templateId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Resume findOwnedResume(Integer resumeId, Integer userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume", "resumeId", resumeId));

        if (!resume.getUserId().equals(userId)) {
            log.warn("User {} attempted to access resume {} owned by user {}",
                    userId, resumeId, resume.getUserId());
            throw new AccessDeniedException(
                    "You do not have permission to access this resume");
        }
        return resume;
    }

    /**
     * Forward the same JWT token to section-service so it can identify
     * the same authenticated user and delete all sections for the resume.
     */
    private void deleteSectionsFromSectionService(Integer resumeId, String authorizationHeader) {
        String url = sectionServiceUrl + "/api/v1/sections/resume/" + resumeId + "/all";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        log.info("Deleted all sections for resume {} via section-service", resumeId);
    }

    /**
     * Forward the same JWT token to section-service so it can duplicate
     * the source resume sections into the new duplicated resume.
     */
    private void duplicateSectionsInSectionService(Integer sourceResumeId,
                                                   Integer targetResumeId,
                                                   String authorizationHeader) {
        String url = sectionServiceUrl + "/api/v1/sections/resume/"
                + sourceResumeId + "/duplicate/" + targetResumeId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        log.info("Duplicated sections from resume {} to resume {} via section-service",
                sourceResumeId, targetResumeId);
    }

    private ResumeResponse mapToResponse(Resume resume) {
        return modelMapper.map(resume, ResumeResponse.class);
    }
}

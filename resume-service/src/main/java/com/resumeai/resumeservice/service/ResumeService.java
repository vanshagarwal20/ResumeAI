package com.resumeai.resumeservice.service;

import com.resumeai.resumeservice.dto.request.AtsScoreUpdateRequest;
import com.resumeai.resumeservice.dto.request.CreateResumeRequest;
import com.resumeai.resumeservice.dto.request.UpdateResumeRequest;
import com.resumeai.resumeservice.dto.response.ResumeResponse;

import java.util.List;

/**
 * ResumeService - business contract for resume lifecycle operations.
 */
public interface ResumeService {

    ResumeResponse createResume(Integer userId, String subscriptionPlan,
                                 CreateResumeRequest request);

    ResumeResponse getResumeById(Integer resumeId, Integer userId);

    List<ResumeResponse> getResumesByUser(Integer userId);

    ResumeResponse getMostRecentResume(Integer userId);

    ResumeResponse updateResume(Integer resumeId, Integer userId,
                                 UpdateResumeRequest request);

    /**
     * Deletes the resume and also deletes all related sections
     * from section-service by forwarding the user's JWT.
     */
    void deleteResume(Integer resumeId, Integer userId, String authorizationHeader);

    /**
     * Duplicates the resume and also duplicates all related sections
     * in section-service by forwarding the user's JWT.
     */
    ResumeResponse duplicateResume(Integer resumeId, Integer userId,
                                    String subscriptionPlan, String authorizationHeader);

    void updateAtsScore(Integer resumeId, AtsScoreUpdateRequest request);

    void publishResume(Integer resumeId, Integer userId);

    void unpublishResume(Integer resumeId, Integer userId);

    List<ResumeResponse> getPublicResumes();

    void incrementViewCount(Integer resumeId);

    List<ResumeResponse> getResumesByTemplate(Integer templateId);
}

package com.resumeai.resumeservice.controller;

import com.resumeai.resumeservice.dto.request.AtsScoreUpdateRequest;
import com.resumeai.resumeservice.dto.request.CreateResumeRequest;
import com.resumeai.resumeservice.dto.request.UpdateResumeRequest;
import com.resumeai.resumeservice.dto.response.ApiResponse;
import com.resumeai.resumeservice.dto.response.ResumeResponse;
import com.resumeai.resumeservice.security.JwtAuthenticationFilter;
import com.resumeai.resumeservice.service.ResumeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ResumeController - REST API for resume lifecycle management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ResumeResponse>> createResume(
            @Valid @RequestBody CreateResumeRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);

        ResumeResponse resume = resumeService.createResume(userId, plan, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume created successfully", resume));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getMyResumes(
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        List<ResumeResponse> resumes = resumeService.getResumesByUser(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Resumes retrieved successfully", resumes));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<ResumeResponse>> getMostRecentResume(
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        ResumeResponse resume = resumeService.getMostRecentResume(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Most recent resume retrieved successfully", resume));
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeById(
            @PathVariable Integer resumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        ResumeResponse resume = resumeService.getResumeById(resumeId, userId);
        return ResponseEntity.ok(
                ApiResponse.success("Resume retrieved successfully", resume));
    }

    @PutMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<ResumeResponse>> updateResume(
            @PathVariable Integer resumeId,
            @Valid @RequestBody UpdateResumeRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        ResumeResponse updated = resumeService.updateResume(resumeId, userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Resume updated successfully", updated));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @PathVariable Integer resumeId,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        resumeService.deleteResume(resumeId, userId, authorizationHeader);
        return ResponseEntity.ok(
                ApiResponse.success("Resume deleted successfully"));
    }

    @PostMapping("/{resumeId}/duplicate")
    public ResponseEntity<ApiResponse<ResumeResponse>> duplicateResume(
            @PathVariable Integer resumeId,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);

        ResumeResponse duplicate = resumeService.duplicateResume(
                resumeId, userId, plan, authorizationHeader);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume duplicated successfully", duplicate));
    }

    @PutMapping("/{resumeId}/ats-score")
    public ResponseEntity<ApiResponse<Void>> updateAtsScore(
            @PathVariable Integer resumeId,
            @Valid @RequestBody AtsScoreUpdateRequest request) {

        resumeService.updateAtsScore(resumeId, request);
        return ResponseEntity.ok(
                ApiResponse.success("ATS score updated to " + request.getAtsScore()));
    }

    @PutMapping("/{resumeId}/publish")
    public ResponseEntity<ApiResponse<Void>> publishResume(
            @PathVariable Integer resumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        resumeService.publishResume(resumeId, userId);
        return ResponseEntity.ok(
                ApiResponse.success("Resume published to public gallery"));
    }

    @PutMapping("/{resumeId}/unpublish")
    public ResponseEntity<ApiResponse<Void>> unpublishResume(
            @PathVariable Integer resumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        resumeService.unpublishResume(resumeId, userId);
        return ResponseEntity.ok(
                ApiResponse.success("Resume removed from public gallery"));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getPublicResumes() {
        List<ResumeResponse> publicResumes = resumeService.getPublicResumes();
        return ResponseEntity.ok(
                ApiResponse.success("Public resumes retrieved successfully", publicResumes));
    }

    @PutMapping("/{resumeId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(
            @PathVariable Integer resumeId) {

        resumeService.incrementViewCount(resumeId);
        return ResponseEntity.ok(ApiResponse.success("View count updated"));
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getResumesByTemplate(
            @PathVariable Integer templateId) {

        List<ResumeResponse> resumes = resumeService.getResumesByTemplate(templateId);
        return ResponseEntity.ok(
                ApiResponse.success("Resumes by template retrieved", resumes));
    }

    private Integer extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new IllegalStateException(
                "User ID not found in request attributes. " +
                "Ensure JwtAuthenticationFilter is running before this controller.");
        }
        return (Integer) userId;
    }

    private String extractPlan(HttpServletRequest request) {
        Object plan = request.getAttribute(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE);
        return plan != null ? plan.toString() : "FREE";
    }
}

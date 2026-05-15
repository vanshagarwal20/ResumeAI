package com.resumeai.ai.controller;

import com.resumeai.ai.dto.request.AiExperienceRequest;
import com.resumeai.ai.dto.request.AiSummaryRequest;
import com.resumeai.ai.dto.request.AtsAnalysisRequest;
import com.resumeai.ai.dto.request.CoverLetterRequest;
import com.resumeai.ai.dto.request.RewriteTextRequest;
import com.resumeai.ai.dto.request.TailorResumeRequest;
import com.resumeai.ai.dto.request.TranslateResumeRequest;
import com.resumeai.ai.dto.response.AiRequestResponse;
import com.resumeai.ai.dto.response.AiTextResponse;
import com.resumeai.ai.dto.response.AiUsageResponse;
import com.resumeai.ai.dto.response.ApiResponse;
import com.resumeai.ai.dto.response.AtsAnalysisResponse;
import com.resumeai.ai.security.JwtAuthenticationFilter;
import com.resumeai.ai.service.AiContentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiContentController {

    private final AiContentService aiContentService;

    @PostMapping("/summary/generate")
    public ResponseEntity<ApiResponse<AiTextResponse>> generateSummary(
            @Valid @RequestBody AiSummaryRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI summary generated successfully",
                aiContentService.generateSummary(userId, plan, request)));
    }

    @PostMapping("/experience/generate")
    public ResponseEntity<ApiResponse<AiTextResponse>> generateExperience(
            @Valid @RequestBody AiExperienceRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI experience bullets generated successfully",
                aiContentService.generateExperienceBullets(userId, plan, request)));
    }

    @PostMapping("/ats/analyze")
    public ResponseEntity<ApiResponse<AtsAnalysisResponse>> analyzeAts(
            @Valid @RequestBody AtsAnalysisRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);

        // If the header wasn't forwarded by the gateway, reconstruct it from
        // the JWT that the JwtAuthenticationFilter already validated.
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            String bearer = httpRequest.getHeader("Authorization");
            if (bearer != null && !bearer.isBlank()) {
                authorizationHeader = bearer;
            }
        }

        return ResponseEntity.ok(ApiResponse.success(
                "ATS analysis completed successfully",
                aiContentService.analyzeAtsScore(userId, plan, request, authorizationHeader)));
    }

    @PostMapping("/cover-letter/generate")
    public ResponseEntity<ApiResponse<AiTextResponse>> generateCoverLetter(
            @Valid @RequestBody CoverLetterRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI cover letter generated successfully",
                aiContentService.generateCoverLetter(userId, plan, request)));
    }

    @PostMapping("/text/rewrite")
    public ResponseEntity<ApiResponse<AiTextResponse>> rewriteText(
            @Valid @RequestBody RewriteTextRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI text rewrite completed successfully",
                aiContentService.rewriteText(userId, plan, request)));
    }

    @PostMapping("/resume/tailor")
    public ResponseEntity<ApiResponse<AiTextResponse>> tailorResume(
            @Valid @RequestBody TailorResumeRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI resume tailoring completed successfully",
                aiContentService.tailorResume(userId, plan, request)));
    }

    @PostMapping("/resume/translate")
    public ResponseEntity<ApiResponse<AiTextResponse>> translateResume(
            @Valid @RequestBody TranslateResumeRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI resume translation completed successfully",
                aiContentService.translateResume(userId, plan, request)));
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<AiUsageResponse>> getUsage(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        String plan = extractPlan(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI usage retrieved successfully",
                aiContentService.getUsage(userId, plan)));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<AiRequestResponse>>> getRequestHistory(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "AI request history retrieved successfully",
                aiContentService.getRequestHistory(userId)));
    }

    private Integer extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request attributes");
        }
        return (Integer) userId;
    }

    private String extractPlan(HttpServletRequest request) {
        Object plan = request.getAttribute(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE);
        return plan != null ? plan.toString() : "FREE";
    }
}


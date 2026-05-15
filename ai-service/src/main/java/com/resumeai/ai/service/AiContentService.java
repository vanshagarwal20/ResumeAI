package com.resumeai.ai.service;

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
import com.resumeai.ai.dto.response.AtsAnalysisResponse;

import java.util.List;

public interface AiContentService {

    AiTextResponse generateSummary(Integer userId, String subscriptionPlan, AiSummaryRequest request);

    AiTextResponse generateExperienceBullets(Integer userId, String subscriptionPlan, AiExperienceRequest request);

    AtsAnalysisResponse analyzeAtsScore(Integer userId, String subscriptionPlan, AtsAnalysisRequest request,
                                        String authorizationHeader);

    AiTextResponse generateCoverLetter(Integer userId, String subscriptionPlan, CoverLetterRequest request);

    AiTextResponse rewriteText(Integer userId, String subscriptionPlan, RewriteTextRequest request);

    AiTextResponse tailorResume(Integer userId, String subscriptionPlan, TailorResumeRequest request);

    AiTextResponse translateResume(Integer userId, String subscriptionPlan, TranslateResumeRequest request);

    AiUsageResponse getUsage(Integer userId, String subscriptionPlan);

    List<AiRequestResponse> getRequestHistory(Integer userId);
}


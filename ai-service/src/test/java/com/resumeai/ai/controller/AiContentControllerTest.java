package com.resumeai.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.dto.request.*;
import com.resumeai.ai.dto.response.*;
import com.resumeai.ai.entity.AiRequest.RequestStatus;
import com.resumeai.ai.entity.AiRequest.RequestType;
import com.resumeai.ai.security.JwtAuthenticationFilter;
import com.resumeai.ai.service.AiContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiContentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AiContentService aiContentService;

    @InjectMocks
    private AiContentController controller;

    private static final Integer USER_ID = 1;
    private static final String PLAN = "FREE";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private com.resumeai.ai.dto.response.AiTextResponse buildTextResponse() {
        return com.resumeai.ai.dto.response.AiTextResponse.builder()
                .generatedText("Generated professional summary")
                .modelUsed("gpt-4o")
                .tokenCount(150)
                .build();
    }

    // ── Generate Summary ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/ai/summary/generate — should return generated summary")
    void generateSummary_shouldReturn200() throws Exception {
        AiSummaryRequest request = new AiSummaryRequest();
        request.setJobTitle("Software Engineer");
        request.setSkills("Java, Spring Boot, Microservices");
        request.setExperienceLevel("Senior");

        when(aiContentService.generateSummary(eq(USER_ID), eq(PLAN), any(AiSummaryRequest.class)))
                .thenReturn(buildTextResponse());

        mockMvc.perform(post("/api/v1/ai/summary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generatedText").exists());

        verify(aiContentService).generateSummary(eq(USER_ID), eq(PLAN), any(AiSummaryRequest.class));
    }

    // ── Generate Experience ──────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/ai/experience/generate — should return experience bullets")
    void generateExperience_shouldReturn200() throws Exception {
        AiExperienceRequest request = new AiExperienceRequest();
        request.setJobTitle("Backend Developer");
        request.setCompanyName("Google");
        request.setWorkSummary("Built microservices and REST APIs");

        when(aiContentService.generateExperienceBullets(eq(USER_ID), eq(PLAN), any(AiExperienceRequest.class)))
                .thenReturn(buildTextResponse());

        mockMvc.perform(post("/api/v1/ai/experience/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedText").exists());
    }

    // ── ATS Analyze ─────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/ai/ats/analyze — should return ATS analysis")
    void analyzeAts_shouldReturn200() throws Exception {
        AtsAnalysisRequest request = new AtsAnalysisRequest();
        request.setResumeText("Java Spring Boot developer with 5 years experience");
        request.setJobDescription("Looking for a Java developer with Spring experience");
        request.setResumeId(10);

        AtsAnalysisResponse atsResponse = AtsAnalysisResponse.builder()
                .resumeId(10)
                .atsScore(75)
                .matchedKeywords(List.of("java", "spring", "developer"))
                .missingKeywords(List.of("kubernetes"))
                .recommendation("Add missing keywords")
                .suggestions(List.of("Add kubernetes to skills"))
                .summary("75% match")
                .build();

        when(aiContentService.analyzeAtsScore(eq(USER_ID), eq(PLAN), any(AtsAnalysisRequest.class), anyString()))
                .thenReturn(atsResponse);

        mockMvc.perform(post("/api/v1/ai/ats/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer test-token")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.atsScore").value(75))
                .andExpect(jsonPath("$.data.matchedKeywords.length()").value(3));
    }

    // ── Cover Letter ─────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/ai/cover-letter/generate — should return cover letter")
    void generateCoverLetter_shouldReturn200() throws Exception {
        CoverLetterRequest request = new CoverLetterRequest();
        request.setResumeText("Resume text here");
        request.setJobDescription("Job description here");
        request.setCompanyName("Google");

        when(aiContentService.generateCoverLetter(eq(USER_ID), eq(PLAN), any(CoverLetterRequest.class)))
                .thenReturn(buildTextResponse());

        mockMvc.perform(post("/api/v1/ai/cover-letter/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedText").exists());
    }

    // ── Rewrite Text ─────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/ai/text/rewrite — should return rewritten text")
    void rewriteText_shouldReturn200() throws Exception {
        RewriteTextRequest request = new RewriteTextRequest();
        request.setContent("I worked at company doing stuff");
        request.setTone("professional");

        when(aiContentService.rewriteText(eq(USER_ID), eq(PLAN), any(RewriteTextRequest.class)))
                .thenReturn(buildTextResponse());

        mockMvc.perform(post("/api/v1/ai/text/rewrite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedText").exists());
    }

    // ── Tailor Resume ────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/ai/resume/tailor — should return tailored resume")
    void tailorResume_shouldReturn200() throws Exception {
        TailorResumeRequest request = new TailorResumeRequest();
        request.setResumeJson("{\"sections\":[]}");
        request.setJobDescription("Job description");

        when(aiContentService.tailorResume(eq(USER_ID), eq(PLAN), any(TailorResumeRequest.class)))
                .thenReturn(buildTextResponse());

        mockMvc.perform(post("/api/v1/ai/resume/tailor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedText").exists());
    }

    // ── Translate Resume ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/ai/resume/translate — should return translated resume")
    void translateResume_shouldReturn200() throws Exception {
        TranslateResumeRequest request = new TranslateResumeRequest();
        request.setResumeJson("{\"sections\":[]}");
        request.setTargetLanguage("Hindi");

        when(aiContentService.translateResume(eq(USER_ID), eq(PLAN), any(TranslateResumeRequest.class)))
                .thenReturn(buildTextResponse());

        mockMvc.perform(post("/api/v1/ai/resume/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedText").exists());
    }

    // ── Usage ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/ai/usage — should return AI usage statistics")
    void getUsage_shouldReturn200() throws Exception {
        AiUsageResponse usage = AiUsageResponse.builder()
                .userId(USER_ID)
                .subscriptionPlan(PLAN)
                .requestsUsedThisMonth(3)
                .atsChecksUsedThisMonth(1)
                .freeTierMonthlyLimit(5)
                .freeTierAtsMonthlyLimit(3)
                .requestsRemaining(2)
                .atsChecksRemaining(2)
                .tokensUsedThisMonth(500)
                .isPremium(false)
                .build();

        when(aiContentService.getUsage(USER_ID, PLAN)).thenReturn(usage);

        mockMvc.perform(get("/api/v1/ai/usage")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestsUsedThisMonth").value(3))
                .andExpect(jsonPath("$.data.requestsRemaining").value(2));
    }

    // ── Request History ──────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/ai/requests — should return request history")
    void getRequestHistory_shouldReturn200() throws Exception {
        AiRequestResponse historyItem = AiRequestResponse.builder()
                .aiRequestId(1)
                .requestType(RequestType.SUMMARY)
                .modelUsed("gpt-4o")
                .tokenCount(150)
                .status(RequestStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(aiContentService.getRequestHistory(USER_ID)).thenReturn(List.of(historyItem));

        mockMvc.perform(get("/api/v1/ai/requests")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].requestType").value("SUMMARY"));
    }
}

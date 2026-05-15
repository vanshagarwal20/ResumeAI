package com.resumeai.ai.service.impl;

import com.resumeai.ai.dto.request.AiExperienceRequest;
import com.resumeai.ai.dto.request.AiSummaryRequest;
import com.resumeai.ai.dto.request.AtsAnalysisRequest;
import com.resumeai.ai.dto.response.AiRequestResponse;
import com.resumeai.ai.dto.response.AiTextResponse;
import com.resumeai.ai.dto.response.AiUsageResponse;
import com.resumeai.ai.dto.response.AtsAnalysisResponse;
import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.entity.AiRequest.RequestStatus;
import com.resumeai.ai.entity.AiRequest.RequestType;
import com.resumeai.ai.entity.AiUsage;
import com.resumeai.ai.exception.AiQuotaExceededException;
import com.resumeai.ai.repository.AiRequestRepository;
import com.resumeai.ai.repository.AiUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiContentServiceImplTest {

    @Mock
    private AiUsageRepository aiUsageRepository;

    @Mock
    private AiRequestRepository aiRequestRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AiContentServiceImpl aiContentService;

    private static final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiContentService, "freeTierMonthlyLimit", 5);
        ReflectionTestUtils.setField(aiContentService, "freeTierAtsMonthlyLimit", 3);
        ReflectionTestUtils.setField(aiContentService, "resumeServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(aiContentService, "openAiApiKey", "");
        ReflectionTestUtils.setField(aiContentService, "openAiModel", "gpt-4o");
        ReflectionTestUtils.setField(aiContentService, "openAiUrl", "https://api.openai.com/v1/chat/completions");
        ReflectionTestUtils.setField(aiContentService, "anthropicApiKey", "");
        ReflectionTestUtils.setField(aiContentService, "anthropicModel", "claude-3-5-sonnet-20241022");
        ReflectionTestUtils.setField(aiContentService, "anthropicUrl", "https://api.anthropic.com/v1/messages");
    }

    private void mockRedisAndUsage(int requestsUsed, int atsChecksUsed) {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);

        AiUsage usage = AiUsage.builder()
                .userId(USER_ID)
                .usageMonth(LocalDateTime.now().getMonthValue())
                .usageYear(LocalDateTime.now().getYear())
                .requestsUsed(requestsUsed)
                .atsChecksUsed(atsChecksUsed)
                .tokensUsed(100)
                .build();

        lenient().when(aiUsageRepository.findByUserIdAndUsageMonthAndUsageYear(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(Optional.of(usage));
        lenient().when(aiUsageRepository.save(any(AiUsage.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── generateSummary ─────────────────────────────────────

    @Test
    @DisplayName("generateSummary — FREE user under limit should use fallback when no API key")
    void generateSummary_freeUser_fallback() {
        mockRedisAndUsage(2, 0);

        AiSummaryRequest request = new AiSummaryRequest();
        request.setJobTitle("Software Engineer");
        request.setSkills("Java, Spring Boot");
        request.setExperienceLevel("Senior");

        AiTextResponse result = aiContentService.generateSummary(USER_ID, "FREE", request);

        assertThat(result).isNotNull();
        assertThat(result.getGeneratedText()).containsIgnoringCase("Software Engineer");
        assertThat(result.getModelUsed()).isEqualTo("local-template");
        verify(aiRequestRepository).save(any(AiRequest.class));
    }

    @Test
    @DisplayName("generateSummary — PREMIUM user should bypass quota check")
    void generateSummary_premiumUser() {
        // PREMIUM should skip Redis quota check entirely, but still record usage in DB
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(aiUsageRepository.findByUserIdAndUsageMonthAndUsageYear(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(Optional.of(AiUsage.builder()
                        .userId(USER_ID)
                        .usageMonth(LocalDateTime.now().getMonthValue())
                        .usageYear(LocalDateTime.now().getYear())
                        .requestsUsed(100)
                        .atsChecksUsed(50)
                        .tokensUsed(50000)
                        .build()));
        when(aiUsageRepository.save(any(AiUsage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AiSummaryRequest request = new AiSummaryRequest();
        request.setJobTitle("CTO");
        request.setSkills("Leadership, Strategy");
        request.setExperienceLevel("Executive");

        AiTextResponse result = aiContentService.generateSummary(USER_ID, "PREMIUM", request);

        assertThat(result).isNotNull();
        assertThat(result.getGeneratedText()).isNotEmpty();
    }

    @Test
    @DisplayName("generateSummary — FREE user at limit should throw AiQuotaExceededException")
    void generateSummary_freeUserAtLimit() {
        mockRedisAndUsage(5, 0);

        AiSummaryRequest request = new AiSummaryRequest();
        request.setJobTitle("Dev");
        request.setSkills("Java");
        request.setExperienceLevel("Mid");

        assertThatThrownBy(() -> aiContentService.generateSummary(USER_ID, "FREE", request))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessageContaining("content");
    }

    // ── generateExperienceBullets ────────────────────────────

    @Test
    @DisplayName("generateExperienceBullets — should return fallback bullets")
    void generateExperienceBullets_fallback() {
        mockRedisAndUsage(1, 0);

        AiExperienceRequest request = new AiExperienceRequest();
        request.setJobTitle("Backend Developer");
        request.setCompanyName("Google");
        request.setWorkSummary("Built microservices using Java and Spring Boot");

        AiTextResponse result = aiContentService.generateExperienceBullets(USER_ID, "FREE", request);

        assertThat(result.getGeneratedText()).contains("Backend Developer");
        assertThat(result.getGeneratedText()).contains("Google");
    }

    // ── analyzeAtsScore ─────────────────────────────────────

    @Test
    @DisplayName("analyzeAtsScore — should compute score based on keyword matching")
    void analyzeAtsScore_shouldComputeScore() {
        mockRedisAndUsage(0, 1);

        AtsAnalysisRequest request = new AtsAnalysisRequest();
        request.setResumeId(10);
        request.setResumeText("java spring boot microservices kubernetes docker rest api");
        request.setJobDescription("looking for java spring boot developer with microservices experience");

        AtsAnalysisResponse result = aiContentService.analyzeAtsScore(USER_ID, "FREE", request, null);

        assertThat(result).isNotNull();
        assertThat(result.getAtsScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.getAtsScore()).isLessThanOrEqualTo(100);
        assertThat(result.getMatchedKeywords()).isNotEmpty();
        assertThat(result.getSuggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("analyzeAtsScore — FREE user at ATS limit should throw")
    void analyzeAtsScore_freeUserAtAtsLimit() {
        mockRedisAndUsage(0, 3);

        AtsAnalysisRequest request = new AtsAnalysisRequest();
        request.setResumeText("resume text");
        request.setJobDescription("job description");

        assertThatThrownBy(() -> aiContentService.analyzeAtsScore(USER_ID, "FREE", request, null))
                .isInstanceOf(AiQuotaExceededException.class)
                .hasMessageContaining("ATS");
    }

    // ── getUsage ─────────────────────────────────────────────

    @Test
    @DisplayName("getUsage — FREE user should return correct remaining counts")
    void getUsage_freeUser() {
        AiUsage usage = AiUsage.builder()
                .userId(USER_ID)
                .usageMonth(LocalDateTime.now().getMonthValue())
                .usageYear(LocalDateTime.now().getYear())
                .requestsUsed(3)
                .atsChecksUsed(1)
                .tokensUsed(500)
                .build();

        when(aiUsageRepository.findByUserIdAndUsageMonthAndUsageYear(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(Optional.of(usage));

        AiUsageResponse result = aiContentService.getUsage(USER_ID, "FREE");

        assertThat(result.getRequestsUsedThisMonth()).isEqualTo(3);
        assertThat(result.getRequestsRemaining()).isEqualTo(2);
        assertThat(result.getAtsChecksRemaining()).isEqualTo(2);
        assertThat(result.getIsPremium()).isFalse();
    }

    @Test
    @DisplayName("getUsage — PREMIUM user should have unlimited remaining")
    void getUsage_premiumUser() {
        when(aiUsageRepository.findByUserIdAndUsageMonthAndUsageYear(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        AiUsageResponse result = aiContentService.getUsage(USER_ID, "PREMIUM");

        assertThat(result.getIsPremium()).isTrue();
        assertThat(result.getRequestsRemaining()).isEqualTo(Integer.MAX_VALUE);
        assertThat(result.getAtsChecksRemaining()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("getUsage — new user with no usage should return zero counts")
    void getUsage_newUser() {
        when(aiUsageRepository.findByUserIdAndUsageMonthAndUsageYear(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        AiUsageResponse result = aiContentService.getUsage(USER_ID, "FREE");

        assertThat(result.getRequestsUsedThisMonth()).isEqualTo(0);
        assertThat(result.getAtsChecksUsedThisMonth()).isEqualTo(0);
        assertThat(result.getRequestsRemaining()).isEqualTo(5);
    }

    // ── getRequestHistory ────────────────────────────────────

    @Test
    @DisplayName("getRequestHistory — should return mapped list")
    void getRequestHistory_shouldReturnList() {
        AiRequest aiRequest = AiRequest.builder()
                .aiRequestId(1)
                .userId(USER_ID)
                .requestType(RequestType.SUMMARY)
                .modelUsed("gpt-4o")
                .tokenCount(150)
                .status(RequestStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(aiRequestRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(aiRequest));

        List<AiRequestResponse> result = aiContentService.getRequestHistory(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRequestType()).isEqualTo(RequestType.SUMMARY);
        assertThat(result.get(0).getModelUsed()).isEqualTo("gpt-4o");
    }
}

package com.resumeai.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.resumeai.ai.entity.AiRequest;
import com.resumeai.ai.entity.AiRequest.RequestStatus;
import com.resumeai.ai.entity.AiRequest.RequestType;
import com.resumeai.ai.entity.AiUsage;
import com.resumeai.ai.exception.AiQuotaExceededException;
import com.resumeai.ai.repository.AiRequestRepository;
import com.resumeai.ai.repository.AiUsageRepository;
import com.resumeai.ai.service.AiContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiContentServiceImpl implements AiContentService {

    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
            "the", "and", "for", "with", "are", "was", "were", "have", "has", "had",
            "this", "that", "these", "those", "from", "into", "onto", "about", "your",
            "their", "there", "here", "will", "would", "should", "could", "can",
            "job", "role", "looking", "need", "needs", "want", "wants", "required",
            "require", "requires", "using", "used", "use", "you", "our", "who",
            "how", "why", "when", "where", "what", "which", "while", "also", "than",
            "then", "them", "they", "been", "being", "over", "under", "more", "less",
            "very", "such", "through", "across", "between", "within", "without",
            "including", "include", "includes", "etc", "all", "any", "each", "every",
            "some", "not", "but"
    ));

    private final AiUsageRepository aiUsageRepository;
    private final AiRequestRepository aiRequestRepository;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.free-tier-monthly-limit:5}")
    private int freeTierMonthlyLimit;

    @Value("${ai.free-tier-ats-monthly-limit:3}")
    private int freeTierAtsMonthlyLimit;

    @Value("${services.resume-service-url}")
    private String resumeServiceUrl;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-4o}")
    private String openAiModel;

    @Value("${ai.openai.url:https://api.openai.com/v1/chat/completions}")
    private String openAiUrl;

    @Value("${ai.anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${ai.anthropic.model:claude-3-5-sonnet-20241022}")
    private String anthropicModel;

    @Value("${ai.anthropic.url:https://api.anthropic.com/v1/messages}")
    private String anthropicUrl;

    @Override
    @Transactional
    public AiTextResponse generateSummary(Integer userId, String subscriptionPlan, AiSummaryRequest request) {
        String prompt = "Create a concise professional resume summary for job title "
                + sanitize(request.getJobTitle()) + ", experience level "
                + sanitize(request.getExperienceLevel()) + ", and skills " + sanitize(request.getSkills()) + ".";
        String fallback = String.format(
                "Results-driven %s with %s experience and strong expertise in %s. "
                        + "Passionate about building scalable, secure, and production-ready solutions "
                        + "that improve business outcomes.",
                sanitize(request.getJobTitle()),
                sanitize(request.getExperienceLevel()),
                sanitize(request.getSkills()).toLowerCase());
        return executeTextRequest(userId, subscriptionPlan, RequestType.SUMMARY, prompt, fallback);
    }

    @Override
    @Transactional
    public AiTextResponse generateExperienceBullets(Integer userId, String subscriptionPlan, AiExperienceRequest request) {
        String company = request.getCompanyName() == null || request.getCompanyName().isBlank()
                ? "the company"
                : sanitize(request.getCompanyName());
        String prompt = "Generate four impact-focused resume bullet points for a "
                + sanitize(request.getJobTitle()) + " at " + company
                + " from this work summary: " + sanitize(request.getWorkSummary());
        String fallback = "- Worked as " + sanitize(request.getJobTitle()) + " at " + company + "\n"
                + "- Delivered backend features and improved application reliability\n"
                + "- Key contribution: " + sanitize(request.getWorkSummary()) + "\n"
                + "- Collaborated with team members to ship scalable business solutions";
        return executeTextRequest(userId, subscriptionPlan, RequestType.EXPERIENCE, prompt, fallback);
    }

    @Override
    @Transactional
    public AtsAnalysisResponse analyzeAtsScore(Integer userId, String subscriptionPlan, AtsAnalysisRequest request,
                                               String authorizationHeader) {
        consumeQuota(userId, subscriptionPlan, true, estimateTokens(request.getResumeText() + request.getJobDescription()));

        Integer resumeId = request.getResumeId();
        if (resumeId == null && authorizationHeader != null && !authorizationHeader.isBlank()) {
            try {
                resumeId = fetchMostRecentResumeId(authorizationHeader);
            } catch (Exception ex) {
                log.warn("Could not fetch most recent resume: {}", ex.getMessage());
            }
        }

        Set<String> resumeWords = tokenize(request.getResumeText());
        Set<String> jobWords = tokenize(request.getJobDescription());

        List<String> matchedKeywords = jobWords.stream().filter(resumeWords::contains).limit(12).toList();
        List<String> missingKeywords = jobWords.stream().filter(word -> !resumeWords.contains(word)).limit(12).toList();

        int totalKeywords = Math.max(jobWords.size(), 1);
        int atsScore = (int) Math.round((matchedKeywords.size() * 100.0) / totalKeywords);
        atsScore = Math.max(0, Math.min(100, atsScore));

        // Update resume ATS score — non-blocking: don't let this crash the analysis
        if (resumeId != null) {
            try {
                updateResumeAtsScore(resumeId, atsScore);
            } catch (Exception ex) {
                log.warn("Failed to update ATS score on resume {}: {}", resumeId, ex.getMessage());
            }
        }

        String recommendation = missingKeywords.isEmpty()
                ? "Strong ATS alignment. Resume already covers the main job keywords."
                : "Add more job-specific keywords such as: " + String.join(", ", missingKeywords);

        // Build suggestions list for the frontend
        List<String> suggestions = new java.util.ArrayList<>();
        if (!missingKeywords.isEmpty()) {
            suggestions.add("Add these missing keywords to your resume: " + String.join(", ", missingKeywords));
            suggestions.add("Tailor your experience bullet points to include terminology from the job description.");
        }
        if (atsScore < 60) {
            suggestions.add("Your resume match is below 60%. Consider restructuring your skills section to better align with the job requirements.");
            suggestions.add("Use the exact phrases from the job posting rather than synonyms for better ATS parsing.");
        } else if (atsScore < 80) {
            suggestions.add("Good match! Fine-tune your resume by incorporating a few more job-specific keywords into your experience descriptions.");
        } else {
            suggestions.add("Excellent match! Your resume is well-aligned with this job description.");
        }

        String summary = String.format(
                "Your resume matches %d%% of the job requirements. %d keywords matched, %d keywords missing.",
                atsScore, matchedKeywords.size(), missingKeywords.size());

        String responseText = "ATS score " + atsScore + ". " + recommendation;
        persistAiRequest(userId, RequestType.ATS, "keyword-extraction", sanitize(request.getJobDescription()),
                responseText, estimateTokens(request.getResumeText() + request.getJobDescription()),
                RequestStatus.COMPLETED, null);

        return AtsAnalysisResponse.builder()
                .resumeId(resumeId)
                .atsScore(atsScore)
                .matchedKeywords(matchedKeywords)
                .missingKeywords(missingKeywords)
                .recommendation(recommendation)
                .suggestions(suggestions)
                .summary(summary)
                .build();
    }

    @Override
    @Transactional
    public AiTextResponse generateCoverLetter(Integer userId, String subscriptionPlan, CoverLetterRequest request) {
        String prompt = "Write a personalized cover letter using this resume text: "
                + sanitize(request.getResumeText()) + "\nFor this job description: "
                + sanitize(request.getJobDescription()) + "\nCompany: " + sanitize(request.getCompanyName());
        String fallback = "Dear Hiring Team,\n\nI am excited to apply for this opportunity. "
                + "My background aligns with the role's core requirements, and I can bring relevant skills, "
                + "ownership, and measurable impact to your team.\n\nSincerely,\n";
        return executeTextRequest(userId, subscriptionPlan, RequestType.COVER_LETTER, prompt, fallback);
    }

    @Override
    @Transactional
    public AiTextResponse rewriteText(Integer userId, String subscriptionPlan, RewriteTextRequest request) {
        String tone = request.getTone() == null || request.getTone().isBlank()
                ? "professional and concise"
                : sanitize(request.getTone());
        String prompt = "Rewrite this resume content in a " + tone
                + " tone while preserving facts: " + sanitize(request.getContent());
        return executeTextRequest(userId, subscriptionPlan, RequestType.REWRITE, prompt,
                "Improved version: " + sanitize(request.getContent()));
    }

    @Override
    @Transactional
    public AiTextResponse tailorResume(Integer userId, String subscriptionPlan, TailorResumeRequest request) {
        String prompt = "Return a revised resume JSON object tailored to the job description.\nResume JSON: "
                + sanitize(request.getResumeJson()) + "\nJob description: " + sanitize(request.getJobDescription());
        return executeTextRequest(userId, subscriptionPlan, RequestType.TAILOR_RESUME, prompt,
                sanitize(request.getResumeJson()));
    }

    @Override
    @Transactional
    public AiTextResponse translateResume(Integer userId, String subscriptionPlan, TranslateResumeRequest request) {
        String prompt = "Translate this resume JSON into " + sanitize(request.getTargetLanguage())
                + " while preserving the JSON structure and professional tone: " + sanitize(request.getResumeJson());
        return executeTextRequest(userId, subscriptionPlan, RequestType.TRANSLATE, prompt,
                sanitize(request.getResumeJson()));
    }

    @Override
    @Transactional(readOnly = true)
    public AiUsageResponse getUsage(Integer userId, String subscriptionPlan) {
        boolean premium = isPremiumPlan(subscriptionPlan);
        LocalDateTime now = LocalDateTime.now();
        AiUsage usage = aiUsageRepository
                .findByUserIdAndUsageMonthAndUsageYear(userId, now.getMonthValue(), now.getYear())
                .orElse(AiUsage.builder()
                        .userId(userId)
                        .usageMonth(now.getMonthValue())
                        .usageYear(now.getYear())
                        .requestsUsed(0)
                        .atsChecksUsed(0)
                        .tokensUsed(0)
                        .build());

        int contentUsed = usage.getRequestsUsed() == null ? 0 : usage.getRequestsUsed();
        int atsUsed = usage.getAtsChecksUsed() == null ? 0 : usage.getAtsChecksUsed();

        return AiUsageResponse.builder()
                .userId(userId)
                .subscriptionPlan(subscriptionPlan)
                .requestsUsedThisMonth(contentUsed)
                .atsChecksUsedThisMonth(atsUsed)
                .freeTierMonthlyLimit(freeTierMonthlyLimit)
                .freeTierAtsMonthlyLimit(freeTierAtsMonthlyLimit)
                .requestsRemaining(premium ? Integer.MAX_VALUE : Math.max(0, freeTierMonthlyLimit - contentUsed))
                .atsChecksRemaining(premium ? Integer.MAX_VALUE : Math.max(0, freeTierAtsMonthlyLimit - atsUsed))
                .tokensUsedThisMonth(usage.getTokensUsed() == null ? 0 : usage.getTokensUsed())
                .isPremium(premium)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiRequestResponse> getRequestHistory(Integer userId) {
        return aiRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(request -> AiRequestResponse.builder()
                        .aiRequestId(request.getAiRequestId())
                        .requestType(request.getRequestType())
                        .modelUsed(request.getModelUsed())
                        .tokenCount(request.getTokenCount())
                        .status(request.getStatus())
                        .errorMessage(request.getErrorMessage())
                        .createdAt(request.getCreatedAt())
                        .build())
                .toList();
    }

    private AiTextResponse executeTextRequest(Integer userId, String subscriptionPlan, RequestType requestType,
                                              String prompt, String fallback) {
        int tokenCount = estimateTokens(prompt);
        consumeQuota(userId, subscriptionPlan, false, tokenCount);

        String modelUsed = "local-template";
        String response = fallback;
        String error = null;

        try {
            AiProviderResponse providerResponse = callOpenAi(prompt);
            modelUsed = providerResponse.modelUsed();
            response = providerResponse.content();
            tokenCount = providerResponse.tokenCount();
        } catch (Exception primaryException) {
            try {
                AiProviderResponse providerResponse = callAnthropic(prompt);
                modelUsed = providerResponse.modelUsed();
                response = providerResponse.content();
                tokenCount = providerResponse.tokenCount();
            } catch (Exception secondaryException) {
                error = primaryException.getMessage() == null
                        ? secondaryException.getMessage()
                        : primaryException.getMessage();
            }
        }

        persistAiRequest(userId, requestType, modelUsed, prompt, response, tokenCount, RequestStatus.COMPLETED, error);
        return AiTextResponse.builder()
                .generatedText(response)
                .modelUsed(modelUsed)
                .tokenCount(tokenCount)
                .build();
    }

    private void consumeQuota(Integer userId, String subscriptionPlan, boolean atsQuota, int tokensUsed) {
        boolean premium = isPremiumPlan(subscriptionPlan);
        int limit = atsQuota ? freeTierAtsMonthlyLimit : freeTierMonthlyLimit;
        if (!premium) {
            int currentCount = getCurrentUsageCount(userId, atsQuota);
            if (currentCount >= limit) {
                throw new AiQuotaExceededException("FREE plan monthly "
                        + (atsQuota ? "ATS" : "content")
                        + " AI request limit reached. Upgrade to PREMIUM for unlimited AI usage.");
            }
            incrementRedisUsage(userId, atsQuota);
        }

        LocalDateTime now = LocalDateTime.now();
        AiUsage usage = aiUsageRepository
                .findByUserIdAndUsageMonthAndUsageYear(userId, now.getMonthValue(), now.getYear())
                .orElseGet(() -> aiUsageRepository.save(AiUsage.builder()
                        .userId(userId)
                        .usageMonth(now.getMonthValue())
                        .usageYear(now.getYear())
                        .requestsUsed(0)
                        .atsChecksUsed(0)
                        .tokensUsed(0)
                        .build()));

        if (atsQuota) {
            usage.setAtsChecksUsed((usage.getAtsChecksUsed() == null ? 0 : usage.getAtsChecksUsed()) + 1);
        } else {
            usage.setRequestsUsed((usage.getRequestsUsed() == null ? 0 : usage.getRequestsUsed()) + 1);
        }
        usage.setTokensUsed((usage.getTokensUsed() == null ? 0 : usage.getTokensUsed()) + Math.max(tokensUsed, 0));
        usage.setLastRequestAt(now);
        aiUsageRepository.save(usage);
    }

    private int getCurrentUsageCount(Integer userId, boolean atsQuota) {
        String redisKey = redisQuotaKey(userId, atsQuota);
        try {
            Object cachedCount = redisTemplate.opsForValue().get(redisKey);
            if (cachedCount != null) {
                return Integer.parseInt(cachedCount.toString());
            }
        } catch (RuntimeException ignored) {
        }

        LocalDateTime now = LocalDateTime.now();
        AiUsage dbUsage = aiUsageRepository
                .findByUserIdAndUsageMonthAndUsageYear(userId, now.getMonthValue(), now.getYear())
                .orElse(null);
        int currentCount = dbUsage == null
                ? 0
                : atsQuota
                    ? (dbUsage.getAtsChecksUsed() == null ? 0 : dbUsage.getAtsChecksUsed())
                    : (dbUsage.getRequestsUsed() == null ? 0 : dbUsage.getRequestsUsed());

        try {
            redisTemplate.opsForValue().set(redisKey, String.valueOf(currentCount), monthTtl());
        } catch (RuntimeException ignored) {
        }
        return currentCount;
    }

    private void incrementRedisUsage(Integer userId, boolean atsQuota) {
        try {
            redisTemplate.opsForValue().increment(redisQuotaKey(userId, atsQuota));
        } catch (RuntimeException ignored) {
        }
    }

    private String redisQuotaKey(Integer userId, boolean atsQuota) {
        return "ai_quota:" + (atsQuota ? "ats:" : "content:") + userId + ":" + YearMonth.now();
    }

    private Duration monthTtl() {
        LocalDate lastDay = YearMonth.now().atEndOfMonth();
        return Duration.between(LocalDateTime.now(), lastDay.atTime(23, 59, 59));
    }

    private boolean isPremiumPlan(String subscriptionPlan) {
        return "PREMIUM".equalsIgnoreCase(subscriptionPlan);
    }

    private Set<String> tokenize(String input) {
        return Arrays.stream(input.toLowerCase()
                        .replaceAll("[^a-z0-9 ]", " ")
                        .split("\\s+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void updateResumeAtsScore(Integer resumeId, Integer atsScore) {
        String url = resumeServiceUrl + "/api/v1/resumes/" + resumeId + "/ats-score";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"atsScore\":" + atsScore + "}";
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(requestBody, headers), Void.class);
    }

    private Integer fetchMostRecentResumeId(String authorizationHeader) {
        String url = resumeServiceUrl + "/api/v1/resumes/recent";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);

        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                }
        ).getBody();

        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalStateException("No recent resume found for ATS analysis");
        }
        Object resumeId = data.get("resumeId");
        if (resumeId instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException("Recent resume response did not include resumeId");
    }

    private AiProviderResponse callOpenAi(String prompt) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new RestClientException("OpenAI API key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", openAiModel);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are ResumeAI, an expert resume writing assistant."),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.4);

        String raw = restTemplate.postForObject(openAiUrl, new HttpEntity<>(body, headers), String.class);
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            int tokens = root.path("usage").path("total_tokens").asInt(estimateTokens(prompt + content));
            return new AiProviderResponse(content, openAiModel, tokens);
        } catch (Exception ex) {
            throw new RestClientException("Unable to parse OpenAI response", ex);
        }
    }

    private AiProviderResponse callAnthropic(String prompt) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            throw new RestClientException("Anthropic API key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", anthropicModel);
        body.put("max_tokens", 1200);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        String raw = restTemplate.postForObject(anthropicUrl, new HttpEntity<>(body, headers), String.class);
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("content").path(0).path("text").asText();
            int tokens = root.path("usage").path("input_tokens").asInt(0)
                    + root.path("usage").path("output_tokens").asInt(0);
            return new AiProviderResponse(content, anthropicModel,
                    tokens > 0 ? tokens : estimateTokens(prompt + content));
        } catch (Exception ex) {
            throw new RestClientException("Unable to parse Anthropic response", ex);
        }
    }

    private void persistAiRequest(Integer userId, RequestType type, String modelUsed, String prompt,
                                  String response, int tokenCount, RequestStatus status, String errorMessage) {
        aiRequestRepository.save(AiRequest.builder()
                .userId(userId)
                .requestType(type)
                .modelUsed(modelUsed)
                .inputPrompt(prompt)
                .aiResponse(response)
                .tokenCount(Math.max(tokenCount, 0))
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replaceAll("(?i)<script.*?>.*?</script>", " ")
                .trim();
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private record AiProviderResponse(String content, String modelUsed, int tokenCount) {
    }
}

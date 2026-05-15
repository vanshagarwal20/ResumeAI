package com.resumeai.jobmatch.service.impl;

import com.resumeai.jobmatch.dto.request.JobSearchRequest;
import com.resumeai.jobmatch.dto.response.JobListingResponse;
import com.resumeai.jobmatch.dto.response.JobMatchResponse;
import com.resumeai.jobmatch.entity.JobMatch;
import com.resumeai.jobmatch.exception.ResourceNotFoundException;
import com.resumeai.jobmatch.repository.JobMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class JobMatchServiceImplTest {

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private JobMatchServiceImpl jobMatchService;

    private static final Integer USER_ID = 1;
    private JobMatch sampleJobMatch;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jobMatchService, "resumeServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(jobMatchService, "sectionServiceUrl", "http://localhost:8084");

        sampleJobMatch = JobMatch.builder()
                .jobMatchId(1)
                .userId(USER_ID)
                .resumeId(10)
                .jobTitle("Software Engineer")
                .companyName("Google")
                .jobUrl("https://linkedin.com/jobs/view/123")
                .source("LinkedIn")
                .jobDescription("Looking for a Java developer with Spring Boot and microservices experience")
                .matchScore(75)
                .matchedKeywords("java, spring, microservices")
                .missingKeywords("kubernetes, docker")
                .recommendation("Moderate match. Add missing keywords.")
                .bookmarked(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── QUERIES ─────────────────────────────────────────────

    @Test
    @DisplayName("getMyJobMatches — should return all user's matches")
    void getMyJobMatches_shouldReturnList() {
        when(jobMatchRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(sampleJobMatch));

        List<JobMatchResponse> result = jobMatchService.getMyJobMatches(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobTitle()).isEqualTo("Software Engineer");
        assertThat(result.get(0).getMatchedKeywords()).containsExactly("java", "spring", "microservices");
    }

    @Test
    @DisplayName("getJobMatchesByResume — should return matches for specific resume")
    void getJobMatchesByResume_shouldReturnFiltered() {
        when(jobMatchRepository.findByResumeIdAndUserIdOrderByCreatedAtDesc(10, USER_ID))
                .thenReturn(List.of(sampleJobMatch));

        List<JobMatchResponse> result = jobMatchService.getJobMatchesByResume(USER_ID, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResumeId()).isEqualTo(10);
    }

    @Test
    @DisplayName("getJobMatchById — should return match when owned")
    void getJobMatchById_shouldReturnWhenOwned() {
        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(sampleJobMatch));

        JobMatchResponse result = jobMatchService.getJobMatchById(USER_ID, 1);

        assertThat(result.getJobMatchId()).isEqualTo(1);
        assertThat(result.getMatchScore()).isEqualTo(75);
    }

    @Test
    @DisplayName("getJobMatchById — should throw when not found")
    void getJobMatchById_shouldThrowWhenNotFound() {
        when(jobMatchRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobMatchService.getJobMatchById(USER_ID, 999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getJobMatchById — should throw when not owned by user")
    void getJobMatchById_shouldThrowWhenNotOwned() {
        JobMatch otherUserMatch = JobMatch.builder()
                .jobMatchId(1).userId(999).resumeId(10)
                .jobTitle("Test").companyName("Test").source("Manual")
                .jobDescription("desc").matchScore(50)
                .matchedKeywords("").missingKeywords("")
                .recommendation("rec").bookmarked(false)
                .createdAt(LocalDateTime.now()).build();

        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(otherUserMatch));

        assertThatThrownBy(() -> jobMatchService.getJobMatchById(USER_ID, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getTopMatches — should return top N matches sorted by score")
    void getTopMatches_shouldReturnTopN() {
        JobMatch highScore = JobMatch.builder()
                .jobMatchId(2).userId(USER_ID).resumeId(10)
                .jobTitle("Senior Dev").companyName("Meta").source("LinkedIn")
                .jobDescription("desc").matchScore(95)
                .matchedKeywords("java, spring").missingKeywords("")
                .recommendation("Excellent").bookmarked(false)
                .createdAt(LocalDateTime.now()).build();

        when(jobMatchRepository.findByUserIdOrderByMatchScoreDesc(USER_ID))
                .thenReturn(List.of(highScore, sampleJobMatch));

        List<JobMatchResponse> result = jobMatchService.getTopMatches(USER_ID, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMatchScore()).isEqualTo(95);
    }

    // ── BOOKMARKS ───────────────────────────────────────────

    @Test
    @DisplayName("updateBookmark — should set bookmarked to true")
    void updateBookmark_shouldBookmark() {
        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(sampleJobMatch));
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        JobMatchResponse result = jobMatchService.updateBookmark(USER_ID, 1, true);

        assertThat(result.getBookmarked()).isTrue();
        verify(jobMatchRepository).save(any(JobMatch.class));
    }

    @Test
    @DisplayName("updateBookmark — should set bookmarked to false")
    void updateBookmark_shouldUnbookmark() {
        sampleJobMatch.setBookmarked(true);
        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(sampleJobMatch));
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(inv -> inv.getArgument(0));

        JobMatchResponse result = jobMatchService.updateBookmark(USER_ID, 1, false);

        assertThat(result.getBookmarked()).isFalse();
    }

    @Test
    @DisplayName("getBookmarkedMatches — should return only bookmarked matches")
    void getBookmarkedMatches_shouldReturnBookmarked() {
        sampleJobMatch.setBookmarked(true);
        when(jobMatchRepository.findByUserIdAndBookmarkedTrueOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(sampleJobMatch));

        List<JobMatchResponse> result = jobMatchService.getBookmarkedMatches(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookmarked()).isTrue();
    }

    // ── DELETE ──────────────────────────────────────────────

    @Test
    @DisplayName("deleteMatch — should delete owned match")
    void deleteMatch_shouldDelete() {
        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(sampleJobMatch));

        jobMatchService.deleteMatch(USER_ID, 1);

        verify(jobMatchRepository).delete(sampleJobMatch);
    }

    @Test
    @DisplayName("deleteMatch — should throw when not found")
    void deleteMatch_shouldThrowWhenNotFound() {
        when(jobMatchRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobMatchService.deleteMatch(USER_ID, 999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── TAILORING RECOMMENDATIONS ───────────────────────────

    @Test
    @DisplayName("getTailoringRecommendations — excellent match (>=80)")
    void getTailoringRecommendations_excellentMatch() {
        sampleJobMatch.setMatchScore(90);
        sampleJobMatch.setMissingKeywords("");
        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(sampleJobMatch));

        String result = jobMatchService.getTailoringRecommendations(USER_ID, 1);

        assertThat(result).contains("Excellent Match");
        assertThat(result).contains("Fine-tuning suggestions");
    }

    @Test
    @DisplayName("getTailoringRecommendations — moderate match (50-79)")
    void getTailoringRecommendations_moderateMatch() {
        sampleJobMatch.setMatchScore(65);
        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(sampleJobMatch));

        String result = jobMatchService.getTailoringRecommendations(USER_ID, 1);

        assertThat(result).contains("Moderate Match");
        assertThat(result).contains("Key improvements");
    }

    @Test
    @DisplayName("getTailoringRecommendations — low match (<50)")
    void getTailoringRecommendations_lowMatch() {
        sampleJobMatch.setMatchScore(30);
        when(jobMatchRepository.findById(1)).thenReturn(Optional.of(sampleJobMatch));

        String result = jobMatchService.getTailoringRecommendations(USER_ID, 1);

        assertThat(result).contains("Low Match");
        assertThat(result).contains("Critical actions");
    }

    // ── LIVE JOB SEARCH ─────────────────────────────────────

    @Test
    @DisplayName("fetchJobsFromLinkedIn — should return simulated LinkedIn listings")
    void fetchJobsFromLinkedIn_shouldReturnList() {
        List<Map<String, Object>> result = jobMatchService.fetchJobsFromLinkedIn("Java Developer", "Remote");

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).containsKey("jobTitle");
        assertThat(result.get(0)).containsKey("companyName");
        assertThat(result.get(0)).containsKey("salary");
        assertThat(result.get(0).get("jobTitle").toString()).contains("Java Developer");
    }

    @Test
    @DisplayName("fetchJobsFromNaukri — should return simulated Naukri listings")
    void fetchJobsFromNaukri_shouldReturnList() {
        List<Map<String, Object>> result = jobMatchService.fetchJobsFromNaukri("Backend Developer", "Bangalore");

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).containsKey("jobTitle");
        assertThat(result.get(0)).containsKey("salary");
        String salary = result.get(0).get("salary").toString();
        assertThat(salary).contains("₹");
    }

    @Test
    @DisplayName("searchLiveJobs — should combine LinkedIn and Naukri results sorted by relevance")
    void searchLiveJobs_shouldCombineAndSort() {
        JobSearchRequest request = new JobSearchRequest();
        request.setJobTitle("Full Stack Developer");
        request.setLocation("Remote");

        List<JobListingResponse> result = jobMatchService.searchLiveJobs(USER_ID, request);

        assertThat(result).isNotEmpty();
        // Verify sorted by relevance descending
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i - 1).getRelevanceScore())
                    .isGreaterThanOrEqualTo(result.get(i).getRelevanceScore());
        }
    }
}

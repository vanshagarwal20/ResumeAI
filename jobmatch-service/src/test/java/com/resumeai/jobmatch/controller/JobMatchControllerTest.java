package com.resumeai.jobmatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.jobmatch.dto.request.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.request.CreateJobMatchRequest;
import com.resumeai.jobmatch.dto.request.JobSearchRequest;
import com.resumeai.jobmatch.dto.response.JobListingResponse;
import com.resumeai.jobmatch.dto.response.JobMatchResponse;
import com.resumeai.jobmatch.security.JwtAuthenticationFilter;
import com.resumeai.jobmatch.service.JobMatchService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class JobMatchControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JobMatchService jobMatchService;

    @InjectMocks
    private JobMatchController controller;

    private static final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private JobMatchResponse buildJobMatchResponse(Integer id, int score) {
        return JobMatchResponse.builder()
                .jobMatchId(id)
                .userId(USER_ID)
                .resumeId(10)
                .jobTitle("Software Engineer")
                .companyName("Google")
                .jobUrl("https://linkedin.com/jobs/view/123")
                .source("LinkedIn")
                .jobDescription("Looking for a Java developer")
                .matchScore(score)
                .matchedKeywords(List.of("java", "spring", "microservices"))
                .missingKeywords(List.of("kubernetes"))
                .recommendation("Good match")
                .bookmarked(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── CREATE / ANALYZE ────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/job-matches — should create job match")
    void createJobMatch_shouldReturn201() throws Exception {
        CreateJobMatchRequest request = new CreateJobMatchRequest();
        request.setResumeId(10);
        request.setJobTitle("Software Engineer");
        request.setCompanyName("Google");
        request.setJobUrl("https://linkedin.com/jobs/view/123");
        request.setJobDescription("Looking for a Java developer with Spring experience");

        when(jobMatchService.createJobMatch(eq(USER_ID), any(CreateJobMatchRequest.class), anyString()))
                .thenReturn(buildJobMatchResponse(1, 75));

        mockMvc.perform(post("/api/v1/job-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer test-token")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobMatchId").value(1))
                .andExpect(jsonPath("$.data.matchScore").value(75));

        verify(jobMatchService).createJobMatch(eq(USER_ID), any(CreateJobMatchRequest.class), anyString());
    }

    @Test
    @DisplayName("POST /api/v1/job-matches/analyze — should analyze job fit")
    void analyzeJobFit_shouldReturn201() throws Exception {
        AnalyzeJobFitRequest request = new AnalyzeJobFitRequest();
        request.setResumeId(10);
        request.setJobTitle("Backend Developer");
        request.setCompanyName("Meta");
        request.setJobDescription("Building scalable systems");

        when(jobMatchService.analyzeJobFit(eq(USER_ID), any(AnalyzeJobFitRequest.class), anyString()))
                .thenReturn(buildJobMatchResponse(2, 85));

        mockMvc.perform(post("/api/v1/job-matches/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer test-token")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.matchScore").value(85));
    }

    // ── QUERIES ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/job-matches — should return user's matches")
    void getMyJobMatches_shouldReturnList() throws Exception {
        when(jobMatchService.getMyJobMatches(USER_ID))
                .thenReturn(List.of(buildJobMatchResponse(1, 75), buildJobMatchResponse(2, 60)));

        mockMvc.perform(get("/api/v1/job-matches")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/resume/{resumeId} — should return matches for resume")
    void getJobMatchesByResume_shouldReturnList() throws Exception {
        when(jobMatchService.getJobMatchesByResume(USER_ID, 10))
                .thenReturn(List.of(buildJobMatchResponse(1, 75)));

        mockMvc.perform(get("/api/v1/job-matches/resume/10")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/{id} — should return single match")
    void getJobMatchById_shouldReturnMatch() throws Exception {
        when(jobMatchService.getJobMatchById(USER_ID, 1)).thenReturn(buildJobMatchResponse(1, 75));

        mockMvc.perform(get("/api/v1/job-matches/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobMatchId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/top — should return top matches")
    void getTopMatches_shouldReturnSorted() throws Exception {
        when(jobMatchService.getTopMatches(USER_ID, 5))
                .thenReturn(List.of(buildJobMatchResponse(1, 90), buildJobMatchResponse(2, 85)));

        mockMvc.perform(get("/api/v1/job-matches/top")
                        .param("limit", "5")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ── BOOKMARKS ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/job-matches/bookmarks — should return bookmarked matches")
    void getBookmarkedMatches_shouldReturnList() throws Exception {
        JobMatchResponse bookmarked = buildJobMatchResponse(1, 75);
        bookmarked.setBookmarked(true);
        when(jobMatchService.getBookmarkedMatches(USER_ID)).thenReturn(List.of(bookmarked));

        mockMvc.perform(get("/api/v1/job-matches/bookmarks")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bookmarked").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/job-matches/{id}/bookmark — should bookmark match")
    void bookmarkMatch_shouldReturnUpdated() throws Exception {
        JobMatchResponse bookmarked = buildJobMatchResponse(1, 75);
        bookmarked.setBookmarked(true);
        when(jobMatchService.updateBookmark(USER_ID, 1, true)).thenReturn(bookmarked);

        mockMvc.perform(put("/api/v1/job-matches/1/bookmark")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/job-matches/{id}/unbookmark — should unbookmark match")
    void unbookmarkMatch_shouldReturnUpdated() throws Exception {
        when(jobMatchService.updateBookmark(USER_ID, 1, false)).thenReturn(buildJobMatchResponse(1, 75));

        mockMvc.perform(put("/api/v1/job-matches/1/unbookmark")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarked").value(false));
    }

    // ── DELETE ──────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/job-matches/{id} — should delete match")
    void deleteMatch_shouldReturn200() throws Exception {
        doNothing().when(jobMatchService).deleteMatch(USER_ID, 1);

        mockMvc.perform(delete("/api/v1/job-matches/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(jobMatchService).deleteMatch(USER_ID, 1);
    }

    // ── LIVE JOB SEARCH ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/job-matches/search — should return live job listings")
    void searchLiveJobs_shouldReturnList() throws Exception {
        JobSearchRequest request = new JobSearchRequest();
        request.setJobTitle("Java Developer");
        request.setLocation("Remote");

        JobListingResponse listing = JobListingResponse.builder()
                .source("LinkedIn").jobTitle("Java Developer").companyName("Google")
                .location("Remote").jobUrl("https://linkedin.com/jobs/1")
                .relevanceScore(90).description("Java dev role")
                .salary("$120,000 - $180,000").postedDate("2 days ago")
                .employmentType("Full-time").build();

        when(jobMatchService.searchLiveJobs(eq(USER_ID), any(JobSearchRequest.class)))
                .thenReturn(List.of(listing));

        mockMvc.perform(post("/api/v1/job-matches/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].source").value("LinkedIn"));
    }

    @Test
    @DisplayName("POST /api/v1/job-matches/fetch/linkedin — should return LinkedIn jobs")
    void fetchLinkedInJobs_shouldReturnList() throws Exception {
        JobSearchRequest request = new JobSearchRequest();
        request.setJobTitle("Backend Developer");
        request.setLocation("San Francisco");

        when(jobMatchService.fetchJobsFromLinkedIn("Backend Developer", "San Francisco"))
                .thenReturn(List.of(Map.of("jobTitle", "Backend Dev", "companyName", "Google")));

        mockMvc.perform(post("/api/v1/job-matches/fetch/linkedin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/job-matches/fetch/naukri — should return Naukri jobs")
    void fetchNaukriJobs_shouldReturnList() throws Exception {
        JobSearchRequest request = new JobSearchRequest();
        request.setJobTitle("Java Developer");
        request.setLocation("Bangalore");

        when(jobMatchService.fetchJobsFromNaukri("Java Developer", "Bangalore"))
                .thenReturn(List.of(Map.of("jobTitle", "Java Dev", "companyName", "Infosys")));

        mockMvc.perform(post("/api/v1/job-matches/fetch/naukri")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ── TAILORING RECOMMENDATIONS ───────────────────────────

    @Test
    @DisplayName("GET /api/v1/job-matches/{id}/recommendations — should return recommendations")
    void getRecommendations_shouldReturnText() throws Exception {
        when(jobMatchService.getTailoringRecommendations(USER_ID, 1))
                .thenReturn("## Tailoring Recommendations\n- Add missing keywords");

        mockMvc.perform(get("/api/v1/job-matches/1/recommendations")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("## Tailoring Recommendations\n- Add missing keywords"));
    }
}

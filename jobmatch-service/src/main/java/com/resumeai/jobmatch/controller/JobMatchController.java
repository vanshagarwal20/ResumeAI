package com.resumeai.jobmatch.controller;

import com.resumeai.jobmatch.dto.request.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.request.CreateJobMatchRequest;
import com.resumeai.jobmatch.dto.request.JobSearchRequest;
import com.resumeai.jobmatch.dto.response.ApiResponse;
import com.resumeai.jobmatch.dto.response.JobListingResponse;
import com.resumeai.jobmatch.dto.response.JobMatchResponse;
import com.resumeai.jobmatch.security.JwtAuthenticationFilter;
import com.resumeai.jobmatch.service.JobMatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * JobMatch REST resource — exposes all job-match operations.
 *
 * Endpoints:
 *   POST   /api/v1/job-matches              — create match (legacy)
 *   POST   /api/v1/job-matches/analyze       — analyze job fit
 *   GET    /api/v1/job-matches               — list all matches for user
 *   GET    /api/v1/job-matches/resume/{id}   — list matches for one resume
 *   GET    /api/v1/job-matches/{id}          — get single match
 *   GET    /api/v1/job-matches/bookmarks     — bookmarked matches
 *   GET    /api/v1/job-matches/top           — top matches by score
 *   PUT    /api/v1/job-matches/{id}/bookmark — bookmark
 *   PUT    /api/v1/job-matches/{id}/unbookmark
 *   DELETE /api/v1/job-matches/{id}          — delete match
 *   POST   /api/v1/job-matches/search        — search live jobs
 *   POST   /api/v1/job-matches/fetch/linkedin — fetch LinkedIn jobs
 *   POST   /api/v1/job-matches/fetch/naukri   — fetch Naukri jobs
 *   GET    /api/v1/job-matches/{id}/recommendations — tailoring recommendations
 */
@RestController
@RequestMapping("/api/v1/job-matches")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;

    // ── CREATE / ANALYZE ────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<JobMatchResponse>> createJobMatch(
            @Valid @RequestBody CreateJobMatchRequest request,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Job match created successfully",
                        jobMatchService.createJobMatch(userId, request, authorizationHeader)));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<JobMatchResponse>> analyzeJobFit(
            @Valid @RequestBody AnalyzeJobFitRequest request,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Job fit analysis completed successfully",
                        jobMatchService.analyzeJobFit(userId, request, authorizationHeader)));
    }

    // ── QUERIES ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobMatchResponse>>> getMyJobMatches(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Job matches retrieved successfully",
                jobMatchService.getMyJobMatches(userId)));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<ApiResponse<List<JobMatchResponse>>> getJobMatchesByResume(
            @PathVariable Integer resumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Resume job matches retrieved successfully",
                jobMatchService.getJobMatchesByResume(userId, resumeId)));
    }

    @GetMapping("/{jobMatchId}")
    public ResponseEntity<ApiResponse<JobMatchResponse>> getJobMatchById(
            @PathVariable Integer jobMatchId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Job match retrieved successfully",
                jobMatchService.getJobMatchById(userId, jobMatchId)));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<JobMatchResponse>>> getTopMatches(
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Top job matches retrieved successfully",
                jobMatchService.getTopMatches(userId, limit)));
    }

    // ── BOOKMARKS ───────────────────────────────────────────────────────────

    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<List<JobMatchResponse>>> getBookmarkedMatches(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Bookmarked job matches retrieved successfully",
                jobMatchService.getBookmarkedMatches(userId)));
    }

    @PutMapping("/{jobMatchId}/bookmark")
    public ResponseEntity<ApiResponse<JobMatchResponse>> bookmarkMatch(
            @PathVariable Integer jobMatchId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Job match bookmarked successfully",
                jobMatchService.updateBookmark(userId, jobMatchId, true)));
    }

    @PutMapping("/{jobMatchId}/unbookmark")
    public ResponseEntity<ApiResponse<JobMatchResponse>> unbookmarkMatch(
            @PathVariable Integer jobMatchId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Job match removed from bookmarks",
                jobMatchService.updateBookmark(userId, jobMatchId, false)));
    }

    // ── DELETE ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{jobMatchId}")
    public ResponseEntity<ApiResponse<Void>> deleteMatch(
            @PathVariable Integer jobMatchId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        jobMatchService.deleteMatch(userId, jobMatchId);
        return ResponseEntity.ok(ApiResponse.success("Job match deleted successfully"));
    }

    // ── LIVE JOB SEARCH ─────────────────────────────────────────────────────

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<JobListingResponse>>> searchLiveJobs(
            @Valid @RequestBody JobSearchRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Live job listings retrieved successfully",
                jobMatchService.searchLiveJobs(userId, request)));
    }

    @PostMapping("/fetch/linkedin")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> fetchLinkedInJobs(
            @Valid @RequestBody JobSearchRequest request,
            HttpServletRequest httpRequest) {

        extractUserId(httpRequest); // auth check
        return ResponseEntity.ok(ApiResponse.success(
                "LinkedIn job listings fetched successfully",
                jobMatchService.fetchJobsFromLinkedIn(request.getJobTitle(), request.getLocation())));
    }

    @PostMapping("/fetch/naukri")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> fetchNaukriJobs(
            @Valid @RequestBody JobSearchRequest request,
            HttpServletRequest httpRequest) {

        extractUserId(httpRequest); // auth check
        return ResponseEntity.ok(ApiResponse.success(
                "Naukri job listings fetched successfully",
                jobMatchService.fetchJobsFromNaukri(request.getJobTitle(), request.getLocation())));
    }

    // ── TAILORING RECOMMENDATIONS ───────────────────────────────────────────

    @GetMapping("/{jobMatchId}/recommendations")
    public ResponseEntity<ApiResponse<String>> getRecommendations(
            @PathVariable Integer jobMatchId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Tailoring recommendations generated successfully",
                jobMatchService.getTailoringRecommendations(userId, jobMatchId)));
    }

    // ── HELPER ──────────────────────────────────────────────────────────────

    private Integer extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request attributes");
        }
        return (Integer) userId;
    }
}

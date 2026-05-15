package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.dto.request.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.request.CreateJobMatchRequest;
import com.resumeai.jobmatch.dto.request.JobSearchRequest;
import com.resumeai.jobmatch.dto.response.JobListingResponse;
import com.resumeai.jobmatch.dto.response.JobMatchResponse;

import java.util.List;
import java.util.Map;

public interface JobMatchService {

    JobMatchResponse createJobMatch(Integer userId, CreateJobMatchRequest request, String authorizationHeader);

    JobMatchResponse analyzeJobFit(Integer userId, AnalyzeJobFitRequest request, String authorizationHeader);

    List<JobMatchResponse> getMyJobMatches(Integer userId);

    List<JobMatchResponse> getJobMatchesByResume(Integer userId, Integer resumeId);

    JobMatchResponse getJobMatchById(Integer userId, Integer jobMatchId);

    JobMatchResponse updateBookmark(Integer userId, Integer jobMatchId, boolean bookmarked);

    List<JobMatchResponse> getBookmarkedMatches(Integer userId);

    List<JobListingResponse> searchLiveJobs(Integer userId, JobSearchRequest request);

    List<Map<String, Object>> fetchJobsFromLinkedIn(String query, String location);

    List<Map<String, Object>> fetchJobsFromNaukri(String query, String location);

    String getTailoringRecommendations(Integer userId, Integer jobMatchId);

    void deleteMatch(Integer userId, Integer jobMatchId);

    List<JobMatchResponse> getTopMatches(Integer userId, int limit);
}

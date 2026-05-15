package com.resumeai.jobmatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.resumeai.jobmatch.entity.JobMatch;

import java.util.List;
import java.util.Optional;

public interface JobMatchRepository extends JpaRepository<JobMatch, Integer> {

    List<JobMatch> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<JobMatch> findByResumeIdAndUserIdOrderByCreatedAtDesc(Integer resumeId, Integer userId);

    List<JobMatch> findByUserIdAndBookmarkedTrueOrderByCreatedAtDesc(Integer userId);

    List<JobMatch> findByResumeIdOrderByCreatedAtDesc(Integer resumeId);

    Optional<JobMatch> findByJobMatchId(Integer matchId);

    List<JobMatch> findByMatchScoreGreaterThan(Integer score);

    List<JobMatch> findByBookmarked(Boolean bookmarked);

    List<JobMatch> findByJobTitleContainingIgnoreCase(String jobTitle);

    List<JobMatch> findByUserIdOrderByMatchScoreDesc(Integer userId);

    int countByUserId(Integer userId);
}

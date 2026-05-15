package com.resumeai.jobmatch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stores one job match record for one resume.
 * Every time user compares a resume with one job description,
 * one new row can be stored for history.
 */
@Entity
@Table(name = "job_matches",
        indexes = {
                @Index(name = "idx_jobmatch_user", columnList = "user_id"),
                @Index(name = "idx_jobmatch_resume", columnList = "resume_id"),
                @Index(name = "idx_jobmatch_score", columnList = "match_score")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer jobMatchId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "resume_id", nullable = false)
    private Integer resumeId;

    @Column(name = "job_title", nullable = false, length = 200)
    private String jobTitle;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "job_url", length = 1000)
    private String jobUrl;

    @Column(name = "source", length = 80)
    private String source;

    @Lob
    @Column(name = "job_description", nullable = false, columnDefinition = "LONGTEXT")
    private String jobDescription;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @Lob
    @Column(name = "matched_keywords", columnDefinition = "LONGTEXT")
    private String matchedKeywords;

    @Lob
    @Column(name = "missing_keywords", columnDefinition = "LONGTEXT")
    private String missingKeywords;

    @Column(name = "recommendation", length = 1000)
    private String recommendation;

    @Column(name = "bookmarked")
    @Builder.Default
    private Boolean bookmarked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}


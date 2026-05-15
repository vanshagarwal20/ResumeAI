package com.resumeai.jobmatch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JobMatchResponse {
    private Integer jobMatchId;
    private Integer userId;
    private Integer resumeId;
    private String jobTitle;
    private String companyName;
    private String jobUrl;
    private String source;
    private String jobDescription;
    private Integer matchScore;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private String recommendation;
    private Boolean bookmarked;
    private LocalDateTime createdAt;
}


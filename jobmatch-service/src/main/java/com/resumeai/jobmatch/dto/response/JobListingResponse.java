package com.resumeai.jobmatch.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobListingResponse {
    private String source;
    private String jobTitle;
    private String companyName;
    private String location;
    private String jobUrl;
    private Integer relevanceScore;
    private String description;
    private String salary;
    private String postedDate;
    private String employmentType;
}


package com.resumeai.jobmatch.dto.response;

import lombok.Data;

@Data
public class ResumeDataResponse {
    private Integer resumeId;
    private Integer userId;
    private String title;
    private String targetJobTitle;
    private Integer templateId;
    private Integer atsScore;
    private String status;
    private String language;
    private Boolean isPublic;
    private Integer viewCount;
}


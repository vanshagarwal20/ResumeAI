package com.resumeai.resumeservice.dto.response;

import com.resumeai.resumeservice.entity.Resume.ResumeStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ResumeResponse — DTO returned by all resume read operations.
 *
 * This is the "safe view" of a resume — all fields are read-only from
 * the API consumer's perspective. It does NOT expose internal JPA metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeResponse {

    private Integer resumeId;
    private Integer userId;
    private String title;
    private String targetJobTitle;
    private Integer templateId;
    private Integer atsScore;
    private ResumeStatus status;
    private String language;
    private Boolean isPublic;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


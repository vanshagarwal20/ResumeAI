package com.resumeai.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer aiRequestId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 40)
    private RequestType requestType;

    @Column(name = "model_used", nullable = false, length = 80)
    private String modelUsed;

    @Lob
    @Column(name = "input_prompt", nullable = false, columnDefinition = "LONGTEXT")
    private String inputPrompt;

    @Lob
    @Column(name = "ai_response", columnDefinition = "LONGTEXT")
    private String aiResponse;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum RequestType {
        SUMMARY,
        EXPERIENCE,
        ATS,
        COVER_LETTER,
        REWRITE,
        TAILOR_RESUME,
        TRANSLATE
    }

    public enum RequestStatus {
        COMPLETED,
        FAILED
    }
}

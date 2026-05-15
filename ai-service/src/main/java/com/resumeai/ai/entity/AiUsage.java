package com.resumeai.ai.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_usage_user_month_year", columnNames = {"user_id", "usage_month", "usage_year"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer usageId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "usage_month", nullable = false)
    private Integer usageMonth;

    @Column(name = "usage_year", nullable = false)
    private Integer usageYear;

    @Column(name = "requests_used", nullable = false)
    private Integer requestsUsed;

    @Column(name = "ats_checks_used")
    @Builder.Default
    private Integer atsChecksUsed = 0;

    @Column(name = "tokens_used")
    @Builder.Default
    private Integer tokensUsed = 0;

    @Column(name = "last_request_at")
    private LocalDateTime lastRequestAt;
}


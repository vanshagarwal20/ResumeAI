package com.resumeai.ai.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiUsageResponse {
    private Integer userId;
    private String subscriptionPlan;
    private Integer requestsUsedThisMonth;
    private Integer atsChecksUsedThisMonth;
    private Integer freeTierMonthlyLimit;
    private Integer freeTierAtsMonthlyLimit;
    private Integer requestsRemaining;
    private Integer atsChecksRemaining;
    private Integer tokensUsedThisMonth;
    private Boolean isPremium;
}

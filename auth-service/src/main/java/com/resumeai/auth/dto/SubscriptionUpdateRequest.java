package com.resumeai.auth.dto;

import com.resumeai.auth.entity.User.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * SubscriptionUpdateRequest — payload for upgrading or downgrading subscription plan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionUpdateRequest {

    /**
     * The target subscription plan.
     * Must be one of: FREE, PREMIUM
     */
    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlan plan;
}

package com.resumeai.auth.dto;

import com.resumeai.auth.entity.User.AuthProvider;
import com.resumeai.auth.entity.User.Role;
import com.resumeai.auth.entity.User.SubscriptionPlan;
import lombok.*;

import java.time.LocalDateTime;

/**
 * UserProfileResponse — safe, read-only view of a user's profile.
 *
 * This is a DTO (Data Transfer Object) — it contains ONLY the fields
 * we want to expose over the API. Critically, it does NOT include
 * passwordHash or any sensitive internal fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private AuthProvider provider;
    private Boolean isActive;
    private SubscriptionPlan subscriptionPlan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
   
    
    
}


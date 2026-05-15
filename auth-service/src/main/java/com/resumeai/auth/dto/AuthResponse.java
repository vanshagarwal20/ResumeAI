package com.resumeai.auth.dto;

import lombok.*;

/**
 * AuthResponse — returned after successful registration or login.
 *
 * Contains the JWT access token and refresh token.
 * The client stores these (typically in memory or httpOnly cookies)
 * and sends the access token in the Authorization header for subsequent requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** JWT access token — expires in 24 hours */
    private String accessToken;

    /** JWT refresh token — used to get a new access token without re-login (7 days) */
    private String refreshToken;

    /** Token type prefix for Authorization header — always "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Basic user info so the frontend can immediately render the dashboard */
    private UserProfileResponse user;
}


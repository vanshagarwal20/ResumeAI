package com.resumeai.auth.controller;


import com.resumeai.auth.dto.ApiResponse;
import com.resumeai.auth.util.JwtTokenProvider;
import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.SubscriptionUpdateRequest;
import com.resumeai.auth.dto.UpdateProfileRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AuthController — REST API for authentication and user management.
 *
 * All endpoints are under /api/v1/auth
 *
 * Public endpoints (no JWT required):
 *   POST /api/v1/auth/register     — Create new account
 *   POST /api/v1/auth/login        — Login with email/password
 *   POST /api/v1/auth/refresh      — Refresh JWT tokens
 *   GET  /api/v1/auth/validate     — Validate a token (for other services)
 *
 * Protected endpoints (JWT required):
 *   GET  /api/v1/auth/profile      — Get my profile
 *   PUT  /api/v1/auth/profile      — Update my profile
 *   PUT  /api/v1/auth/password     — Change my password
 *   PUT  /api/v1/auth/subscription — Update my subscription plan
 *   DELETE /api/v1/auth/deactivate — Deactivate my account
 *
 * Admin endpoints (JWT + ADMIN role required):
 *   GET  /api/v1/auth/admin/users  — Get all users
 *   PUT  /api/v1/auth/admin/users/{id}/reactivate — Reactivate suspended user
 *   PUT  /api/v1/auth/admin/users/{id}/subscription — Change user's plan
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    // ══════════════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS — No authentication required
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/v1/auth/register
     *
     * Register a new user with email and password.
     *
     * Request body:
     * {
     *   "fullName": "John Doe",
     *   "email": "john@example.com",
     *   "password": "securePass123",
     *   "phone": "+1234567890"
     * }
     *
     * Response: 201 Created with AuthResponse (JWT tokens + user profile)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Register request received for email: {}", request.getEmail());
        AuthResponse authResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful! Welcome to ResumeAI.", authResponse));
    }

    /**
     * POST /api/v1/auth/login
     *
     * Login with email and password.
     *
     * Request body:
     * {
     *   "email": "john@example.com",
     *   "password": "securePass123"
     * }
     *
     * Response: 200 OK with AuthResponse (JWT tokens + user profile)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request for email: {}", request.getEmail());
        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful!", authResponse));
    }

    /**
     * POST /api/v1/auth/refresh
     *
     * Issue a new access token using a valid refresh token.
     *
     * Request body: { "refreshToken": "eyJhbGci..." }
     * Response: 200 OK with new AuthResponse
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody String refreshToken) {

        // Strip surrounding quotes if sent as a JSON string
        String token = refreshToken.replace("\"", "").trim();
        AuthResponse authResponse = authService.refreshToken(token);

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", authResponse));
    }
    
    /**
     * POST /api/v1/auth/logout
     *
     * Revokes the refresh token and blacklists the current access token.
     * After this call, both tokens are immediately invalid server-side.
     *
     * This endpoint requires authentication (user must send their access token).
     *
     * Response: 200 OK
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {

        // Strip "Bearer " prefix to get the raw token
        String accessToken = authHeader.substring(7);

        // Extract userId from the token (jwtTokenProvider already exists in your codebase)
        Integer userId = jwtTokenProvider.extractUserId(accessToken);

        authService.logout(userId, accessToken);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully. Tokens have been revoked."));
    }

    /**
     * GET /api/v1/auth/validate?token=eyJhbGci...
     *
     * Validate a JWT token — called by other microservices (e.g., API gateway)
     * to verify the token is legitimate before processing a request.
     *
     * Response: { "valid": true/false }
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(
            @RequestParam("token") String token) {

        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(
                ApiResponse.success("Token validation result", isValid));
    }

    // ══════════════════════════════════════════════════════════════
    // AUTHENTICATED USER ENDPOINTS — JWT required
    // ══════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/auth/profile
     *
     * Get the currently authenticated user's profile.
     *
     * Uses @AuthenticationPrincipal to automatically inject the
     * Spring Security UserDetails (loaded by JwtAuthenticationFilter).
     *
     * Response: 200 OK with UserProfileResponse
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserProfileResponse profile = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", profile));
    }

    /**
     * GET /api/v1/auth/profile/{userId}
     *
     * Get any user's profile by ID.
     * Used by other microservices to look up user info.
     *
     * Response: 200 OK with UserProfileResponse
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileById(
            @PathVariable Integer userId) {

        UserProfileResponse profile = authService.getUserById(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", profile));
    }

    /**
     * PUT /api/v1/auth/profile
     *
     * Update the currently authenticated user's profile.
     * Only non-null fields are updated (PATCH semantics).
     *
     * Request body:
     * {
     *   "fullName": "John Updated",
     *   "phone": "+9876543210"
     * }
     *
     * Response: 200 OK with updated UserProfileResponse
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        // Get the authenticated user's ID from their profile
        UserProfileResponse current = authService.getUserByEmail(userDetails.getUsername());
        UserProfileResponse updated = authService.updateProfile(current.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully", updated));
    }

    /**
     * PUT /api/v1/auth/password
     *
     * Change the authenticated user's password.
     *
     * Request body:
     * {
     *   "currentPassword": "oldPass123",
     *   "newPassword": "newSecurePass456"
     * }
     *
     * Response: 200 OK with success message
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        UserProfileResponse current = authService.getUserByEmail(userDetails.getUsername());
        authService.changePassword(current.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully"));
    }

    /**
     * PUT /api/v1/auth/subscription
     *
     * Update the authenticated user's subscription plan.
     * In production, this is called after a successful payment.
     *
     * Request body: { "plan": "PREMIUM" }
     * Response: 200 OK
     */
    @PutMapping("/subscription")
    public ResponseEntity<ApiResponse<Void>> updateSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubscriptionUpdateRequest request) {

        UserProfileResponse current = authService.getUserByEmail(userDetails.getUsername());
        authService.updateSubscription(current.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Subscription updated to " + request.getPlan()));
    }

    /**
     * DELETE /api/v1/auth/deactivate
     *
     * Soft-delete the currently authenticated user's account.
     * The account is suspended (isActive = false) but data is preserved.
     *
     * Response: 200 OK
     */
    @DeleteMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserProfileResponse current = authService.getUserByEmail(userDetails.getUsername());
        authService.deactivateAccount(current.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Account deactivated successfully"));
    }

    // ══════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS — JWT + ADMIN role required
    // ══════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/auth/admin/users
     *
     * Get all registered users — Admin panel user list.
     * Requires ADMIN role (enforced by @PreAuthorize).
     *
     * Response: 200 OK with List<UserProfileResponse>
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {

        List<UserProfileResponse> users = authService.getAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * PUT /api/v1/auth/admin/users/{userId}/reactivate
     *
     * Reactivate a suspended user account.
     * Requires ADMIN role.
     *
     * Response: 200 OK
     */
    @PutMapping("/admin/users/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @PathVariable Integer userId) {

        authService.reactivateAccount(userId);
        return ResponseEntity.ok(
                ApiResponse.success("User account reactivated successfully"));
    }

    /**
     * PUT /api/v1/auth/admin/users/{userId}/deactivate
     *
     * Suspend a user account (Admin action).
     * Requires ADMIN role.
     *
     * Response: 200 OK
     */
    @PutMapping("/admin/users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable Integer userId) {

        authService.deactivateAccount(userId);
        return ResponseEntity.ok(
                ApiResponse.success("User account deactivated successfully"));
    }

    /**
     * PUT /api/v1/auth/admin/users/{userId}/subscription
     *
     * Change any user's subscription plan (Admin override).
     * Requires ADMIN role.
     *
     * Request body: { "plan": "PREMIUM" }
     * Response: 200 OK
     */
    @PutMapping("/admin/users/{userId}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateUserSubscription(
            @PathVariable Integer userId,
            @Valid @RequestBody SubscriptionUpdateRequest request) {

        authService.updateSubscription(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("User subscription updated to " + request.getPlan()));
    }
}

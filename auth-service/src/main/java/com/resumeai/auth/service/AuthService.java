package com.resumeai.auth.service;


import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.SubscriptionUpdateRequest;
import com.resumeai.auth.dto.UpdateProfileRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.entity.User;

import java.util.List;

/**
 * AuthService — Business contract for all authentication and user management operations.
 *
 * This interface defines WHAT the service does, not HOW.
 * The implementation (AuthServiceImpl) provides the HOW.
 *
 * Benefits of interface-based design:
 *  1. Easy to swap implementations (e.g., mock for testing)
 *  2. Enforces a clear contract
 *  3. Supports Spring's @Transactional proxy mechanism
 */
public interface AuthService {

    /**
     * Register a new user with email and password.
     *
     * Steps:
     *  1. Check email is not already taken
     *  2. Hash the password with BCrypt
     *  3. Save the user with FREE plan by default
     *  4. Generate and return JWT tokens
     *
     * @param request Registration form data
     * @return AuthResponse containing JWT tokens + user profile
     * @throws com.resumeai.auth.exception.EmailAlreadyExistsException if email is taken
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate a user with email and password.
     *
     * Steps:
     *  1. Load user by email
     *  2. Verify password against BCrypt hash
     *  3. Check account is active (not suspended)
     *  4. Generate and return JWT tokens
     *
     * @param request Login credentials
     * @return AuthResponse containing JWT tokens + user profile
     * @throws com.resumeai.auth.exception.InvalidCredentialsException if credentials are wrong
     */
    AuthResponse login(LoginRequest request);

    /**
     * Issue a new access token using a valid refresh token.
     * The client calls this when the access token expires (HTTP 401 received).
     *
     * @param refreshToken The refresh token string
     * @return AuthResponse with a new access token
     * @throws com.resumeai.auth.exception.InvalidCredentialsException if refresh token is invalid
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Validate a JWT access token.
     * Used by other microservices (via HTTP) to verify token authenticity.
     *
     * @param token The JWT string to validate
     * @return true if the token is valid and not expired
     */
    boolean validateToken(String token);

    /**
     * Get a user's full profile by their user ID.
     *
     * @param userId The user's database ID
     * @return UserProfileResponse with all non-sensitive fields
     * @throws com.resumeai.auth.exception.ResourceNotFoundException if user not found
     */
    UserProfileResponse getUserById(Integer userId);

    /**
     * Get a user's full profile by their email address.
     *
     * @param email The user's email address
     * @return UserProfileResponse
     * @throws com.resumeai.auth.exception.ResourceNotFoundException if user not found
     */
    UserProfileResponse getUserByEmail(String email);

    /**
     * Update a user's profile information (name, email, phone).
     * Only non-null fields in the request are updated (PATCH semantics).
     *
     * @param userId  The user to update (from JWT)
     * @param request The fields to update
     * @return Updated UserProfileResponse
     */
    UserProfileResponse updateProfile(Integer userId, UpdateProfileRequest request);

    /**
     * Change a user's password.
     * Requires the current password for verification.
     *
     * @param userId  The user to update (from JWT)
     * @param request Contains currentPassword and newPassword
     * @throws com.resumeai.auth.exception.InvalidCredentialsException if current password is wrong
     */
    void changePassword(Integer userId, ChangePasswordRequest request);

    /**
     * Update a user's subscription plan (FREE ↔ PREMIUM).
     * In a real system this would be triggered by a payment webhook.
     *
     * @param userId  The user to update
     * @param request Contains the target plan
     */
    void updateSubscription(Integer userId, SubscriptionUpdateRequest request);

    /**
     * Soft-delete a user account (sets isActive = false).
     * The user can no longer log in but their data is preserved.
     *
     * @param userId The user to deactivate
     */
    void deactivateAccount(Integer userId);

    /**
     * Reactivate a suspended user account (Admin only).
     *
     * @param userId The user to reactivate
     */
    void reactivateAccount(Integer userId);

    /**
     * Get all users — for the Admin panel.
     *
     * @return List of all user profiles
     */
    List<UserProfileResponse> getAllUsers();
    
    void logout(Integer userId, String accessToken);

    /**
     * Process OAuth2 login/registration for Google or LinkedIn.
     * If the user already has an account linked to this provider, log them in.
     * If not, create a new account automatically.
     *
     * @param email      Email from OAuth2 provider
     * @param fullName   Full name from OAuth2 provider
     * @param providerId Provider-specific unique user ID
     * @param provider   GOOGLE or LINKEDIN
     * @return AuthResponse with JWT tokens
     */
    AuthResponse processOAuth2Login(String email, String fullName,
                                     String providerId, User.AuthProvider provider);
}

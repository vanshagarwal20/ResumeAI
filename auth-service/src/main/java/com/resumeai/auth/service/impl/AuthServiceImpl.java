package com.resumeai.auth.service.impl;


import com.resumeai.auth.dto.AuthResponse;
import java.time.Duration;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.SubscriptionUpdateRequest;
import com.resumeai.auth.dto.UpdateProfileRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.entity.User.AuthProvider;
import com.resumeai.auth.entity.User.SubscriptionPlan;
import com.resumeai.auth.exception.*;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.service.AuthService;
import com.resumeai.auth.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AuthServiceImpl — Concrete implementation of AuthService.
 *
 * Contains all business logic for authentication and user management.
 *
 * Key annotations:
 *  @Service     — Marks this as a Spring-managed service bean
 *  @Transactional — Wraps each method in a DB transaction (auto-rollback on exception)
 *  @RequiredArgsConstructor — Lombok: generates constructor injection for all final fields
 *  @Slf4j       — Lombok: injects a 'log' logger
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // ── Dependencies (injected via constructor by Spring) ────────
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final ModelMapper modelMapper;
	private final TokenService tokenService;              // Entity ↔ DTO mapper

    // ── Registration ─────────────────────────────────────────────

    /**
     * Register a new user with email and password.
     *
     * Flow:
     *  1. Validate email uniqueness
     *  2. Hash password with BCrypt (cost factor 10 by default)
     *  3. Create User entity with FREE plan
     *  4. Save to DB
     *  5. Generate JWT tokens
     *  6. Return AuthResponse
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Step 1: Check if email is already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Step 2 & 3: Build User entity with hashed password
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())  // Normalize email
                .passwordHash(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .phone(request.getPhone())
                .role(User.Role.USER)
                .provider(AuthProvider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)   // All new users start FREE
                .isActive(true)
                .build();

        // Step 4: Persist to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        // Step 5 & 6: Generate tokens and return
        AuthResponse response = buildAuthResponse(savedUser);

        // NEW: Store the refresh token in Redis after registration
        tokenService.storeRefreshToken(
            savedUser.getUserId(),
            response.getRefreshToken(),
            Duration.ofMillis(604800000)
        );

        return response;
    }

    // ── Login ────────────────────────────────────────────────────

    /**
     * Authenticate a user with email and password.
     *
     * Flow:
     *  1. Find user by email (fail fast with vague error if not found)
     *  2. Check account is active
     *  3. Verify password against BCrypt hash
     *  4. Generate and return JWT tokens
     *
     * Security note: We use the same error message for "email not found"
     * and "wrong password" to prevent email enumeration attacks.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // NEW: Block brute-force — check failed attempts before touching DB
        if (tokenService.isRateLimited(request.getEmail())) {
            long retryAfter = tokenService.getRateLimitTtlSeconds(request.getEmail());
            throw new InvalidCredentialsException(
                "Too many failed attempts. Try again in " + retryAfter + " seconds.");
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseGet(() -> {
                    // Record failed attempt and then throw
                    tokenService.recordFailedLogin(request.getEmail());
                    throw new InvalidCredentialsException();
                });

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Your account has been suspended. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // NEW: Record failed attempt on wrong password
            tokenService.recordFailedLogin(request.getEmail());
            throw new InvalidCredentialsException();
        }

        // NEW: Successful login — clear any previous failed attempts
        tokenService.clearFailedLogins(request.getEmail());

        log.info("User logged in successfully: {}", user.getUserId());
        AuthResponse response = buildAuthResponse(user);

        // NEW: Store refresh token in Redis (makes it revocable on logout)
        // 604800000ms = 7 days (matches jwt.refresh-expiration-ms in your yml)
        tokenService.storeRefreshToken(
            user.getUserId(),
            response.getRefreshToken(),
            Duration.ofMillis(604800000)
        );

        return response;
    }

    // ── Token Refresh ────────────────────────────────────────────

    /**
     * Issue a new access token using a valid refresh token.
     *
     * Called when the client receives a 401 response (expired access token).
     * The client sends the refresh token to get a new access token without
     * requiring the user to log in again.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.extractEmail(refreshToken);
        Integer userId = jwtTokenProvider.extractUserId(refreshToken);

        // NEW: Verify this is the refresh token we actually issued (prevents reuse of old tokens)
        if (!tokenService.isValidRefreshToken(userId, refreshToken)) {
            throw new InvalidCredentialsException("Refresh token has been revoked or is no longer valid");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Account is suspended");
        }

        AuthResponse response = buildAuthResponse(user);

        // NEW: Rotate the refresh token — revoke old, store new
        // This means each refresh token can only be used ONCE
        tokenService.revokeRefreshToken(userId);
        tokenService.storeRefreshToken(
            userId,
            response.getRefreshToken(),
            Duration.ofMillis(604800000)
        );

        log.info("Refreshed token for user: {}", user.getUserId());
        return response;
    }
    
    
    /**
     * Logout — revokes the refresh token and blacklists the access token.
     * After calling this, both tokens are immediately dead server-side.
     *
     * @param userId      The authenticated user's ID (from JWT)
     * @param accessToken The current access token (will be blacklisted)
     */
    @Override
    public void logout(Integer userId, String accessToken) {
        // 1. Revoke the refresh token — user can no longer get new access tokens
        tokenService.revokeRefreshToken(userId);

        // 2. Blacklist the access token so it can't be used for its remaining validity
        //    jwt.expiration-ms = 86400000 (24h). We blacklist for the full 24h to be safe.
        tokenService.blacklistToken(accessToken, Duration.ofHours(24));

        log.info("User {} logged out — tokens revoked", userId);
    }

    // ── Token Validation ─────────────────────────────────────────

    /**
     * Validate a JWT access token.
     * Used by API Gateway or other microservices.
     */
    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    // ── Profile Management ───────────────────────────────────────

    /**
     * Get user profile by ID.
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Integer userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        return mapToProfileResponse(user);
    }

    /**
     * Get user profile by email.
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToProfileResponse(user);
    }

    /**
     * Update profile fields (PATCH semantics — only update non-null fields).
     *
     * If the user wants to change their email, we first check the new email
     * is not already taken by another account.
     */
    @Override
    @Transactional
    public UserProfileResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // Update only the fields that were provided in the request
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().toLowerCase().trim();
            // Check the new email isn't already taken by someone else
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new EmailAlreadyExistsException(newEmail);
            }
            user.setEmail(newEmail);
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        User updated = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return mapToProfileResponse(updated);
    }

    /**
     * Change password — requires current password verification.
     */
    @Override
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        // OAuth2 users don't have a local password
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new InvalidCredentialsException(
                "Password change is not available for " + user.getProvider() + " accounts");
        }

        // Verify the current password before allowing the change
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Hash and save the new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", userId);
    }

    // ── Subscription Management ──────────────────────────────────

    /**
     * Update the subscription plan for a user.
     * In production, this would be called by a payment webhook handler.
     */
    @Override
    @Transactional
    public void updateSubscription(Integer userId, SubscriptionUpdateRequest request) {
        // Verify user exists before updating
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }
        userRepository.updateSubscriptionPlan(userId, request.getPlan());
        log.info("Subscription updated to {} for user: {}", request.getPlan(), userId);
    }

    // ── Account Lifecycle ────────────────────────────────────────

    /**
     * Soft-delete (suspend) a user account.
     * The user data is preserved; they just cannot log in.
     */
    @Override
    @Transactional
    public void deactivateAccount(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }
        userRepository.updateIsActive(userId, false);
        log.info("Account deactivated for user: {}", userId);
    }

    /**
     * Reactivate a previously suspended account (Admin only).
     */
    @Override
    @Transactional
    public void reactivateAccount(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }
        userRepository.updateIsActive(userId, true);
        log.info("Account reactivated for user: {}", userId);
    }

    /**
     * Get all users — for Admin panel.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    // ── OAuth2 Login ─────────────────────────────────────────────

    /**
     * Handle OAuth2 login from Google or LinkedIn.
     *
     * Flow:
     *  1. Check if we already have an account for this provider+providerId
     *  2. If yes → log them in (update any changed profile info)
     *  3. If no → check if their email is already registered locally
     *     a. If yes → link the OAuth2 provider to the existing account
     *     b. If no  → create a brand-new account
     *  4. Generate and return JWT tokens
     */
    @Override
    @Transactional
    public AuthResponse processOAuth2Login(String email, String fullName,
                                            String providerId, AuthProvider provider) {
        log.info("OAuth2 login: provider={}, email={}", provider, email);

        // Step 1: Try to find existing account by provider credentials
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // Step 3: Not found by provider — check by email
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                // Step 3a: Link OAuth2 to existing local account
                                existingUser.setProvider(provider);
                                existingUser.setProviderId(providerId);
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> {
                                // Step 3b: Create entirely new account
                                User newUser = User.builder()
                                        .fullName(fullName)
                                        .email(email.toLowerCase().trim())
                                        .provider(provider)
                                        .providerId(providerId)
                                        .role(User.Role.USER)
                                        .subscriptionPlan(SubscriptionPlan.FREE)
                                        .isActive(true)
                                        .build();
                                return userRepository.save(newUser);
                            });
                });

        // Step 2: Update name if it changed on the provider side
        if (fullName != null && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            user = userRepository.save(user);
        }

        // Check account is active
        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Your account has been suspended.");
        }

        log.info("OAuth2 login successful for user: {}", user.getUserId());
        return buildAuthResponse(user);
    }

    // ── Private Helpers ──────────────────────────────────────────

    /**
     * Build an AuthResponse from a User entity.
     * Called after successful registration, login, or token refresh.
     *
     * @param user The authenticated user
     * @return AuthResponse with access token, refresh token, and profile
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToProfileResponse(user))
                .build();
    }

    /**
     * Convert a User entity to a UserProfileResponse DTO.
     * Uses ModelMapper to copy matching fields automatically.
     *
     * @param user The User entity
     * @return UserProfileResponse safe for API exposure
     */
    private UserProfileResponse mapToProfileResponse(User user) {
        return modelMapper.map(user, UserProfileResponse.class);
    }
}


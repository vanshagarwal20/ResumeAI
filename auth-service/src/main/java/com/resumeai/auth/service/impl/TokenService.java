package com.resumeai.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * TokenService — Redis-backed token and security management.
 *
 * Replaces the stateless refresh token system in your current AuthServiceImpl.
 * Currently your refresh tokens are stateless JWTs — once issued they can NEVER
 * be revoked until they expire (7 days). This class fixes that.
 *
 * Redis key naming (all readable in redis-cli):
 *   refresh:{userId}          → the refresh token string for that user
 *   blacklist:{tokenHashCode} → marker that this access token was revoked
 *   login_fail:{email}        → count of failed login attempts
 *   otp:{email}:{purpose}     → OTP code (for future email verification)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Key prefixes — keep them short but readable
    private static final String REFRESH_KEY    = "refresh:";
    private static final String BLACKLIST_KEY  = "blacklist:";
    private static final String LOGIN_FAIL_KEY = "login_fail:";
    private static final String OTP_KEY        = "otp:";

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESH TOKEN STORAGE
    // Called after every login() and register() in AuthServiceImpl
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Store a refresh token for a user in Redis.
     * Only ONE active refresh token per user — replaces the old one.
     *
     * @param userId       Your User.userId (Integer from your User entity)
     * @param refreshToken The JWT refresh token string
     * @param ttl          Must match jwt.refresh-expiration-ms in your yml (604800000ms = 7 days)
     */
    public void storeRefreshToken(Integer userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(REFRESH_KEY + userId, refreshToken, ttl);
        log.debug("Stored refresh token for userId={} TTL={}", userId, ttl);
    }

    /**
     * Validate that the refresh token being used matches what we stored.
     * Prevents reuse of old refresh tokens after rotation.
     */
    public boolean isValidRefreshToken(Integer userId, String providedToken) {
        Object stored = redisTemplate.opsForValue().get(REFRESH_KEY + userId);
        return stored != null && stored.toString().equals(providedToken);
    }

    /**
     * Delete the refresh token (called on logout or token rotation).
     */
    public void revokeRefreshToken(Integer userId) {
        redisTemplate.delete(REFRESH_KEY + userId);
        log.debug("Revoked refresh token for userId={}", userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACCESS TOKEN BLACKLISTING
    // Called on logout so the access token can't be used for its remaining life
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Blacklist an access token immediately.
     * Use this in your logout endpoint so the current access token is dead instantly.
     *
     * @param token        The JWT access token string
     * @param remainingTtl How long until this token naturally expires
     *                     (set to jwt.expiration-ms - time-already-elapsed)
     */
    public void blacklistToken(String token, Duration remainingTtl) {
        // We hash the token to avoid extremely long Redis keys
        String key = BLACKLIST_KEY + Math.abs(token.hashCode());
        redisTemplate.opsForValue().set(key, "revoked", remainingTtl);
        log.debug("Blacklisted token, TTL={}", remainingTtl);
    }

    /**
     * Check if an access token has been blacklisted.
     * Call this in JwtAuthenticationFilter BEFORE processing the request.
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_KEY + Math.abs(token.hashCode());
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN RATE LIMITING
    // Your current login() has no brute-force protection — this adds it
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Record a failed login attempt. Auto-expires after 15 minutes.
     * Returns the total failed attempt count.
     */
    public long recordFailedLogin(String email) {
        String key = LOGIN_FAIL_KEY + email.toLowerCase();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // Set 15-minute window only on the FIRST failure
            redisTemplate.expire(key, Duration.ofMinutes(15));
        }
        return count != null ? count : 1;
    }

    /**
     * Returns true if this email has 5+ failed attempts in the last 15 minutes.
     */
    public boolean isRateLimited(String email) {
        String key = LOGIN_FAIL_KEY + email.toLowerCase();
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return false;
        try {
            return Long.parseLong(val.toString()) >= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * How many seconds until the rate limit window resets.
     * Return this to the client in the error response.
     */
    public long getRateLimitTtlSeconds(String email) {
        String key = LOGIN_FAIL_KEY + email.toLowerCase();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * Clear failed attempts after successful login.
     */
    public void clearFailedLogins(String email) {
        redisTemplate.delete(LOGIN_FAIL_KEY + email.toLowerCase());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP STORAGE (for future email verification / password reset)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Store a one-time password code in Redis.
     *
     * @param email   User's email
     * @param purpose "email_verify" or "password_reset"
     * @param otp     The 6-digit code
     * @param ttl     Typically 10 minutes
     */
    public void storeOtp(String email, String purpose, String otp, Duration ttl) {
        String key = OTP_KEY + email.toLowerCase() + ":" + purpose;
        redisTemplate.opsForValue().set(key, otp, ttl);
    }

    /**
     * Verify and consume the OTP in one atomic step.
     * Returns true if the OTP matched. The OTP is deleted after this call.
     */
    public boolean verifyAndConsumeOtp(String email, String purpose, String providedOtp) {
        String key = OTP_KEY + email.toLowerCase() + ":" + purpose;
        Object stored = redisTemplate.opsForValue().getAndDelete(key);
        return stored != null && stored.toString().equals(providedOtp);
    }
}
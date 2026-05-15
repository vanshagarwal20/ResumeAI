package com.resumeai.auth.util;

import com.resumeai.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtTokenProvider — Handles all JWT creation and validation logic.
 *
 * JWT Structure (3 parts separated by dots):
 *   Header.Payload.Signature
 *
 * Our JWT Payload (Claims) contains:
 *   - sub: user's email (subject)
 *   - userId: user's database ID
 *   - role: USER or ADMIN
 *   - subscriptionPlan: FREE or PREMIUM
 *   - iat: issued-at timestamp
 *   - exp: expiration timestamp
 *
 * The signature uses HMAC-SHA256 with a secret key to prevent tampering.
 */
@Slf4j         // Lombok: injects 'log' logger
@Component     // Spring: makes this a managed bean
public class JwtTokenProvider {

    // ── Configuration (injected from application.yml) ────────────

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;         // Access token expiry (24 hours)

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;     // Refresh token expiry (7 days)

    // ── Token Generation ─────────────────────────────────────────

    /**
     * Generate a JWT access token for the given user.
     *
     * The token embeds userId, role, and subscriptionPlan as custom claims
     * so other services can authorize requests without hitting the database.
     *
     * @param user The authenticated user
     * @return Signed JWT string
     */
    public String generateAccessToken(User user) {
        return buildToken(user, jwtExpirationMs);
    }

    /**
     * Generate a JWT refresh token for the given user.
     *
     * Refresh tokens have a longer lifespan (7 days) and are used only
     * to obtain a new access token when the current one expires.
     *
     * @param user The authenticated user
     * @return Signed JWT refresh token string
     */
    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs);
    }

    /**
     * Internal helper — builds a JWT with all required claims.
     *
     * @param user       The user to embed in the token
     * @param expiryMs   Milliseconds until the token expires
     * @return Signed JWT string
     */
    private String buildToken(User user, long expiryMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                // Standard claims
                .setSubject(user.getEmail())          // "sub" claim — user's email
                .setIssuedAt(now)                      // "iat" claim — when token was created
                .setExpiration(expiry)                 // "exp" claim — when token expires

                // Custom claims — embedded so other services can read them without DB lookups
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().name())
                .claim("subscriptionPlan", user.getSubscriptionPlan().name())
                .claim("fullName", user.getFullName())

                // Sign with HMAC-SHA256 key derived from our secret
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token Validation ─────────────────────────────────────────

    /**
     * Validate a JWT token.
     * Returns true only if the token is:
     *  - Properly signed with our secret
     *  - Not expired
     *  - Well-formed (valid JSON, correct structure)
     *
     * @param token The JWT string to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // This throws exceptions for any invalid condition
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;

        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token type is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // ── Claims Extraction ────────────────────────────────────────

    /**
     * Extract all claims (payload) from a JWT token.
     * Use this when you need multiple fields from a token.
     *
     * @param token A valid JWT string
     * @return Claims object containing all embedded data
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract the subject (email address) from a JWT token.
     *
     * @param token A valid JWT string
     * @return The user's email address
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract the userId from a JWT token's custom claims.
     *
     * @param token A valid JWT string
     * @return The user's database ID
     */
    public Integer extractUserId(String token) {
        return extractAllClaims(token).get("userId", Integer.class);
    }

    /**
     * Extract the role from a JWT token's custom claims.
     *
     * @param token A valid JWT string
     * @return The user's role as a String (e.g., "USER", "ADMIN")
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extract the subscription plan from a JWT token's custom claims.
     *
     * @param token A valid JWT string
     * @return The subscription plan as a String (e.g., "FREE", "PREMIUM")
     */
    public String extractSubscriptionPlan(String token) {
        return extractAllClaims(token).get("subscriptionPlan", String.class);
    }

    // ── Private Helpers ──────────────────────────────────────────

    /**
     * Derive the HMAC signing key from the configured secret string.
     *
     * JJWT requires a Key object, not a raw string.
     * We decode the Base64 secret to bytes and build an HMAC key.
     */
    private Key getSigningKey() {
        // If secret is not Base64-encoded, encode it first
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            // Fallback: treat as plain bytes if not valid Base64
            keyBytes = jwtSecret.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}


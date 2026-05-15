package com.resumeai.resumeservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * JwtTokenProvider — Read-only JWT utility for resume-service.
 *
 * This service does NOT issue tokens — it only VALIDATES and READS tokens
 * that were issued by auth-service. Both services share the same JWT secret.
 *
 * Used by JwtAuthenticationFilter to authenticate every incoming request.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Validate a JWT token — checks signature + expiry.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract all claims from a valid JWT token.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** Extract the user's email (subject) from the token */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Extract the userId custom claim */
    public Integer extractUserId(String token) {
        return extractAllClaims(token).get("userId", Integer.class);
    }

    /** Extract the role custom claim (e.g., "USER", "ADMIN") */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /** Extract the subscriptionPlan custom claim (e.g., "FREE", "PREMIUM") */
    public String extractSubscriptionPlan(String token) {
        return extractAllClaims(token).get("subscriptionPlan", String.class);
    }

    private Key getSigningKey() {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (Exception e) {
            return Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
    }
}


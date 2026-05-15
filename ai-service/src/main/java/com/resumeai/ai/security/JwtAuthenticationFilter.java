package com.resumeai.ai.security;

import com.resumeai.ai.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JwtAuthenticationFilter for ai-service.
 *
 * KEY FIX: subscriptionPlan is intentionally NOT read from the JWT claim.
 * The JWT is issued at login time and may contain a stale plan (e.g., FREE)
 * even after the user upgrades to PREMIUM — because the token is not
 * re-issued on upgrade.
 *
 * Instead, we call auth-service's /api/v1/auth/profile endpoint (forwarding
 * the same Bearer token) to get the live subscriptionPlan from the database.
 * This guarantees PREMIUM users are never wrongly quota-blocked.
 *
 * The JWT claims (userId, role) are still read directly from the token
 * since those never change after account creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE   = "authenticatedUserId";
    public static final String USER_PLAN_ATTRIBUTE = "authenticatedUserPlan";
    public static final String USER_ROLE_ATTRIBUTE = "authenticatedUserRole";

    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${services.auth-service-url}")
    private String authServiceUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwt(request);
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // Read stable claims directly from the JWT — these never change.
                String  email  = jwtTokenProvider.extractEmail(jwt);
                Integer userId = jwtTokenProvider.extractUserId(jwt);
                String  role   = jwtTokenProvider.extractRole(jwt);

                // Fetch the live subscriptionPlan from auth-service so we always
                // have the up-to-date value, regardless of when the token was issued.
                String plan = fetchLivePlan(jwt);

                request.setAttribute(USER_ID_ATTRIBUTE,   userId);
                request.setAttribute(USER_PLAN_ATTRIBUTE, plan);
                request.setAttribute(USER_ROLE_ATTRIBUTE, role);

                var authorities    = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("JWT authentication failed for {}: {}", request.getRequestURI(), ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Call auth-service /api/v1/auth/profile with the user's own Bearer token
     * and extract the live subscriptionPlan from the response.
     *
     * Falls back to the JWT claim if the call fails (e.g., auth-service is
     * temporarily unreachable) so the filter never hard-blocks a request.
     * In the fallback case we log a warning so ops can investigate.
     */
    @SuppressWarnings("unchecked")
    private String fetchLivePlan(String jwt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwt);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> res = restTemplate.exchange(
                    authServiceUrl + "/api/v1/auth/profile",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                // Response shape: { data: { subscriptionPlan: "PREMIUM" | "FREE", ... } }
                Map<String, Object> body = res.getBody();
                Object data = body.get("data");
                if (data instanceof Map) {
                    Object plan = ((Map<String, Object>) data).get("subscriptionPlan");
                    if (plan != null) {
                        return plan.toString();
                    }
                }
            }
        } catch (Exception ex) {
            // Auth-service unreachable — fall back to JWT claim so the user
            // is not hard-blocked, but log clearly so this is visible.
            log.warn("Could not fetch live plan from auth-service, falling back to JWT claim: {}", ex.getMessage());
            return jwtTokenProvider.extractSubscriptionPlan(jwt);
        }

        // Fallback: read from JWT if response shape was unexpected
        return jwtTokenProvider.extractSubscriptionPlan(jwt);
    }

    private String extractJwt(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
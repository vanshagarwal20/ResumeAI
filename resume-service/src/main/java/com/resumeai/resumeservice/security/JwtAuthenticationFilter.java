package com.resumeai.resumeservice.security;

import com.resumeai.resumeservice.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthenticationFilter — validates JWT on every request to resume-service.
 *
 * Unlike auth-service, this filter does NOT load the user from a database.
 * All needed information (userId, role, subscriptionPlan) is already embedded
 * in the JWT claims — making authentication fast and stateless.
 *
 * The authenticated principal stored in SecurityContext is the user's email.
 * The userId, role, and plan are stored in the request attributes for use
 * in the controller via @RequestAttribute or helper methods.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Request attribute names — these are set on the request so controllers
     * can read the user's ID and plan without needing a DB call.
     */
    public static final String USER_ID_ATTRIBUTE   = "authenticatedUserId";
    public static final String USER_PLAN_ATTRIBUTE = "authenticatedUserPlan";
    public static final String USER_ROLE_ATTRIBUTE = "authenticatedUserRole";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractJwt(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Extract all needed claims from the token — NO database call needed
                String email = jwtTokenProvider.extractEmail(jwt);
                Integer userId = jwtTokenProvider.extractUserId(jwt);
                String role = jwtTokenProvider.extractRole(jwt);
                String plan = jwtTokenProvider.extractSubscriptionPlan(jwt);

                // Store userId and plan as request attributes for controller use
                request.setAttribute(USER_ID_ATTRIBUTE, userId);
                request.setAttribute(USER_PLAN_ATTRIBUTE, plan);
                request.setAttribute(USER_ROLE_ATTRIBUTE, role);

                // Build Spring Security Authentication with the user's role
                // The role must have "ROLE_" prefix for hasRole() checks to work
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(
                        email, null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user {} (plan: {}) for {}", email, plan, request.getRequestURI());
            }
        } catch (Exception ex) {
            log.error("JWT authentication failed for request {}: {}",
                    request.getRequestURI(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwt(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}


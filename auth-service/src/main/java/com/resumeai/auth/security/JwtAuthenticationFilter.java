package com.resumeai.auth.security;

import com.resumeai.auth.util.JwtTokenProvider;
import com.resumeai.auth.service.impl.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter — Intercepts every HTTP request to validate the JWT.
 *
 * This filter runs ONCE per request (OncePerRequestFilter) and:
 *  1. Extracts the JWT from the Authorization header ("Bearer <token>")
 *  2. Validates the token (signature, expiry)
 *  3. Loads the user from the database
 *  4. Sets the authentication in Spring Security's SecurityContext
 *
 * After step 4, Spring Security knows "who is making this request" and
 * can enforce role-based access control on the controller methods.
 *
 * If no token is present (or it's invalid), the request continues without
 * authentication — the SecurityConfig then blocks access to protected endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenService tokenService;

    /**
     * Core filter logic — runs for every incoming request.
     *
     * @param request     The incoming HTTP request
     * @param response    The HTTP response
     * @param filterChain The remaining filter chain to continue processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Step 1: Extract JWT from Authorization header
            String jwt = extractJwtFromRequest(request);

            // Step 2: Validate the token (only if one was provided)
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // NEW: Reject tokens that were explicitly revoked (user logged out)
                if (tokenService.isBlacklisted(jwt)) {
                    log.warn("Request rejected — token is blacklisted (user logged out)");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                    return;
                }

                // Step 3: Extract the email and load the full user details
                String email = jwtTokenProvider.extractEmail(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Step 4: Create an authentication object and set it in SecurityContext
                // This tells Spring Security "this request is authenticated as this user"
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,                          // Credentials (not needed after login)
                        userDetails.getAuthorities()   // The user's roles
                    );

                // Attach request details (IP address, session ID) to the authentication
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                // Store in the SecurityContext — this is how Spring knows who the user is
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication for user: {} on path: {}",
                    email, request.getRequestURI());
            }
        } catch (Exception ex) {
            // Don't let filter exceptions break the request — just log and continue
            // The SecurityConfig will handle unauthenticated access appropriately
            log.error("Could not set user authentication in security context", ex);
        }

        // Always continue the filter chain — even if authentication failed
        filterChain.doFilter(request, response);
    }

    /**
     * Extract the JWT token from the Authorization header.
     *
     * The header format is: "Authorization: Bearer eyJhbGci..."
     * We strip the "Bearer " prefix to get just the token.
     *
     * @param request The HTTP request
     * @return The JWT string, or null if no valid Authorization header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Check the header exists and starts with "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Return everything after "Bearer " (7 characters)
            return bearerToken.substring(7);
        }

        return null;
    }
}


package com.resumeai.auth.security;

import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.entity.User.AuthProvider;
import com.resumeai.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2AuthenticationSuccessHandler — called after a successful OAuth2 login.
 *
 * When a user successfully authenticates via Google or LinkedIn, Spring Security
 * calls this handler. We use it to:
 *  1. Extract the user's info from the OAuth2 token
 *  2. Create/retrieve the user in our database
 *  3. Issue our own JWT tokens
 *  4. Redirect to the React frontend with tokens as query params
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    // Injected from app.frontend-url in application.yml
    // Falls back to http://localhost:5173 if env var FRONTEND_URL is not set
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Called by Spring Security after a successful OAuth2 authentication.
     *
     * @param request        The HTTP request
     * @param response       The HTTP response
     * @param authentication Contains the OAuth2 user's profile from the provider
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from OAuth2 attributes
        String email      = extractEmail(oAuth2User);
        String fullName   = extractFullName(oAuth2User);
        String providerId = extractProviderId(oAuth2User);

        // Determine which provider this was based on available attributes
        AuthProvider provider = determineProvider(oAuth2User);

        log.info("OAuth2 success: provider={}, email={}", provider, email);

        // Create/retrieve user and issue JWT tokens
        AuthResponse authResponse = authService.processOAuth2Login(
            email, fullName, providerId, provider);

        // Redirect to the frontend with tokens as query params.
        // The React OAuthCallbackPage reads these from window.location.search.
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth/callback")
                .queryParam("accessToken", authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .build()
                .toUriString();

        log.debug("Redirecting OAuth2 user to: {}", redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // ── Private Helpers ──────────────────────────────────────────

    private String extractEmail(OAuth2User oAuth2User) {
        Object email = oAuth2User.getAttribute("email");
        if (email != null) return email.toString();

        // LinkedIn may use 'emailAddress'
        Object emailAddress = oAuth2User.getAttribute("emailAddress");
        if (emailAddress != null) return emailAddress.toString();

        throw new RuntimeException("Email not provided by OAuth2 provider");
    }

    private String extractFullName(OAuth2User oAuth2User) {
        // Google provides 'name'
        Object name = oAuth2User.getAttribute("name");
        if (name != null) return name.toString();

        // LinkedIn provides 'localizedFirstName' and 'localizedLastName'
        Object firstName = oAuth2User.getAttribute("localizedFirstName");
        Object lastName  = oAuth2User.getAttribute("localizedLastName");
        if (firstName != null) {
            return firstName + (lastName != null ? " " + lastName : "");
        }

        return "User"; // Fallback
    }

    private String extractProviderId(OAuth2User oAuth2User) {
        Object sub = oAuth2User.getAttribute("sub");
        if (sub != null) return sub.toString();

        Object id = oAuth2User.getAttribute("id");
        if (id != null) return id.toString();

        return oAuth2User.getName();
    }

    private AuthProvider determineProvider(OAuth2User oAuth2User) {
        // Google tokens have a 'sub' field; LinkedIn uses 'id'
        if (oAuth2User.getAttribute("sub") != null) {
            return AuthProvider.GOOGLE;
        }
        return AuthProvider.LINKEDIN;
    }
}
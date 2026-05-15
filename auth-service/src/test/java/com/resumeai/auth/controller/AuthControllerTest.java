package com.resumeai.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.auth.dto.*;
import com.resumeai.auth.entity.User.SubscriptionPlan;
import com.resumeai.auth.service.AuthService;
import com.resumeai.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private UserProfileResponse buildProfile() {
        return UserProfileResponse.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .subscriptionPlan(SubscriptionPlan.FREE)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .accessToken("access-token-xyz")
                .refreshToken("refresh-token-xyz")
                .tokenType("Bearer")
                .user(buildProfile())
                .build();
    }

    // ── PUBLIC ENDPOINTS ────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/auth/register — should return 201 with tokens")
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("SecurePass123");
        request.setPhone("+1234567890");

        when(authService.register(any(RegisterRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-xyz"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — should return 200 with tokens")
    void login_shouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("SecurePass123");

        when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh — should return new tokens")
    void refresh_shouldReturnNewTokens() throws Exception {
        when(authService.refreshToken(anyString())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"refresh-token-xyz\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());

        verify(authService).refreshToken(anyString());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout — should revoke tokens")
    void logout_shouldRevokeTokens() throws Exception {
        when(jwtTokenProvider.extractUserId("test-access-token")).thenReturn(1);
        doNothing().when(authService).logout(eq(1), eq("test-access-token"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer test-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(1, "test-access-token");
    }

    @Test
    @DisplayName("GET /api/v1/auth/validate — should validate token")
    void validateToken_shouldReturnResult() throws Exception {
        when(authService.validateToken("some-token")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/validate")
                        .param("token", "some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(authService).validateToken("some-token");
    }

    // ── PROFILE ENDPOINTS ───────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/auth/profile/{userId} — should return profile by ID")
    void getProfileById_shouldReturnProfile() throws Exception {
        when(authService.getUserById(1)).thenReturn(buildProfile());

        mockMvc.perform(get("/api/v1/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }
}

package com.resumeai.auth.service.impl;

import com.resumeai.auth.dto.*;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.entity.User.AuthProvider;
import com.resumeai.auth.entity.User.Role;
import com.resumeai.auth.entity.User.SubscriptionPlan;
import com.resumeai.auth.exception.EmailAlreadyExistsException;
import com.resumeai.auth.exception.InvalidCredentialsException;
import com.resumeai.auth.exception.ResourceNotFoundException;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private UserProfileResponse sampleProfile;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .phone("+1234567890")
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleProfile = UserProfileResponse.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .subscriptionPlan(SubscriptionPlan.FREE)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── Registration ──────────────────────────────────────────

    @Test
    @DisplayName("register — should create user and return tokens")
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("SecurePass123");
        request.setPhone("+1234567890");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(sampleUser)).thenReturn("refresh-token");
        when(modelMapper.map(any(User.class), eq(UserProfileResponse.class))).thenReturn(sampleProfile);

        AuthResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUser().getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
        verify(tokenService).storeRefreshToken(eq(1), eq("refresh-token"), any());
    }

    @Test
    @DisplayName("register — should throw when email already exists")
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPassword("pass");
        request.setFullName("John");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // ── Login ─────────────────────────────────────────────────

    @Test
    @DisplayName("login — should authenticate and return tokens")
    void login_shouldAuthenticateAndReturnTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("SecurePass123");

        when(tokenService.isRateLimited("john@example.com")).thenReturn(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("SecurePass123", "$2a$10$hashedpassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(sampleUser)).thenReturn("refresh-token");
        when(modelMapper.map(any(User.class), eq(UserProfileResponse.class))).thenReturn(sampleProfile);

        AuthResponse result = authService.login(request);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        verify(tokenService).clearFailedLogins("john@example.com");
        verify(tokenService).storeRefreshToken(eq(1), eq("refresh-token"), any());
    }

    @Test
    @DisplayName("login — should throw when rate limited")
    void login_shouldThrowWhenRateLimited() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("pass");

        when(tokenService.isRateLimited("john@example.com")).thenReturn(true);
        when(tokenService.getRateLimitTtlSeconds("john@example.com")).thenReturn(60L);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Too many failed attempts");
    }

    @Test
    @DisplayName("login — should throw on wrong password and record failed attempt")
    void login_shouldThrowOnWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("WrongPass");

        when(tokenService.isRateLimited("john@example.com")).thenReturn(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPass", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenService).recordFailedLogin("john@example.com");
    }

    @Test
    @DisplayName("login — should throw when account is suspended")
    void login_shouldThrowWhenSuspended() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("SecurePass123");

        User suspendedUser = User.builder()
                .userId(1).email("john@example.com").passwordHash("hash")
                .isActive(false).provider(AuthProvider.LOCAL).build();

        when(tokenService.isRateLimited("john@example.com")).thenReturn(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(suspendedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("suspended");
    }

    // ── Token Refresh ─────────────────────────────────────────

    @Test
    @DisplayName("refreshToken — should rotate and return new tokens")
    void refreshToken_shouldRotateTokens() {
        when(jwtTokenProvider.validateToken("old-refresh")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("old-refresh")).thenReturn("john@example.com");
        when(jwtTokenProvider.extractUserId("old-refresh")).thenReturn(1);
        when(tokenService.isValidRefreshToken(1, "old-refresh")).thenReturn(true);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(sampleUser)).thenReturn("new-refresh");
        when(modelMapper.map(any(User.class), eq(UserProfileResponse.class))).thenReturn(sampleProfile);

        AuthResponse result = authService.refreshToken("old-refresh");

        assertThat(result.getAccessToken()).isEqualTo("new-access");
        verify(tokenService).revokeRefreshToken(1);
        verify(tokenService).storeRefreshToken(eq(1), eq("new-refresh"), any());
    }

    @Test
    @DisplayName("refreshToken — should throw when token is revoked")
    void refreshToken_shouldThrowWhenRevoked() {
        when(jwtTokenProvider.validateToken("revoked-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("revoked-token")).thenReturn("john@example.com");
        when(jwtTokenProvider.extractUserId("revoked-token")).thenReturn(1);
        when(tokenService.isValidRefreshToken(1, "revoked-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("revoked-token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ── Logout ────────────────────────────────────────────────

    @Test
    @DisplayName("logout — should revoke refresh and blacklist access token")
    void logout_shouldRevokeTokens() {
        authService.logout(1, "access-token");

        verify(tokenService).revokeRefreshToken(1);
        verify(tokenService).blacklistToken(eq("access-token"), any());
    }

    // ── Token Validation ──────────────────────────────────────

    @Test
    @DisplayName("validateToken — should delegate to JwtTokenProvider")
    void validateToken_shouldDelegate() {
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);

        boolean result = authService.validateToken("token");

        assertThat(result).isTrue();
    }

    // ── Profile Management ────────────────────────────────────

    @Test
    @DisplayName("getUserById — should return profile when found")
    void getUserById_shouldReturn() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        when(modelMapper.map(sampleUser, UserProfileResponse.class)).thenReturn(sampleProfile);

        UserProfileResponse result = authService.getUserById(1);

        assertThat(result.getUserId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUserById — should throw when not found")
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findByUserId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile — should update non-null fields")
    void updateProfile_shouldUpdateFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("John Updated");
        request.setPhone("+9999999999");

        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(modelMapper.map(any(User.class), eq(UserProfileResponse.class))).thenReturn(sampleProfile);

        UserProfileResponse result = authService.updateProfile(1, request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile — should throw when email already taken")
    void updateProfile_shouldThrowWhenEmailTaken() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("taken@example.com");

        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.updateProfile(1, request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // ── Change Password ───────────────────────────────────────

    @Test
    @DisplayName("changePassword — should update password when current matches")
    void changePassword_shouldUpdate() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass");
        request.setNewPassword("NewPass123");

        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass", "$2a$10$hashedpassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$10$newhash");

        authService.changePassword(1, request);

        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("changePassword — should throw when current password is wrong")
    void changePassword_shouldThrowWhenWrong() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Wrong");
        request.setNewPassword("NewPass");

        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Wrong", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1, request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ── Subscription Management ───────────────────────────────

    @Test
    @DisplayName("updateSubscription — should update plan")
    void updateSubscription_shouldUpdate() {
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setPlan(SubscriptionPlan.PREMIUM);

        when(userRepository.existsById(1)).thenReturn(true);

        authService.updateSubscription(1, request);

        verify(userRepository).updateSubscriptionPlan(1, SubscriptionPlan.PREMIUM);
    }

    @Test
    @DisplayName("updateSubscription — should throw when user not found")
    void updateSubscription_shouldThrowWhenNotFound() {
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setPlan(SubscriptionPlan.PREMIUM);

        when(userRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> authService.updateSubscription(999, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Account Lifecycle ─────────────────────────────────────

    @Test
    @DisplayName("deactivateAccount — should set isActive to false")
    void deactivateAccount_shouldDeactivate() {
        when(userRepository.existsById(1)).thenReturn(true);

        authService.deactivateAccount(1);

        verify(userRepository).updateIsActive(1, false);
    }

    @Test
    @DisplayName("reactivateAccount — should set isActive to true")
    void reactivateAccount_shouldReactivate() {
        when(userRepository.existsById(1)).thenReturn(true);

        authService.reactivateAccount(1);

        verify(userRepository).updateIsActive(1, true);
    }

    @Test
    @DisplayName("getAllUsers — should return all users as profiles")
    void getAllUsers_shouldReturnAll() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        when(modelMapper.map(sampleUser, UserProfileResponse.class)).thenReturn(sampleProfile);

        List<UserProfileResponse> result = authService.getAllUsers();

        assertThat(result).hasSize(1);
    }

    // ── OAuth2 Login ──────────────────────────────────────────

    @Test
    @DisplayName("processOAuth2Login — should create new user when not found")
    void processOAuth2Login_newUser() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(sampleUser)).thenReturn("refresh-token");
        when(modelMapper.map(any(User.class), eq(UserProfileResponse.class))).thenReturn(sampleProfile);

        AuthResponse result = authService.processOAuth2Login(
                "john@gmail.com", "John Doe", "google-123", AuthProvider.GOOGLE);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("processOAuth2Login — should link to existing account")
    void processOAuth2Login_linkExisting() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(sampleUser)).thenReturn("refresh-token");
        when(modelMapper.map(any(User.class), eq(UserProfileResponse.class))).thenReturn(sampleProfile);

        AuthResponse result = authService.processOAuth2Login(
                "john@example.com", "John Doe", "google-123", AuthProvider.GOOGLE);

        assertThat(result).isNotNull();
        verify(userRepository, atLeast(1)).save(any(User.class));
    }
}

package com.vokabelnetz.service;

import com.vokabelnetz.config.AppProperties;
import com.vokabelnetz.config.JwtProperties;
import com.vokabelnetz.dto.request.LoginRequest;
import com.vokabelnetz.dto.request.RegisterRequest;
import com.vokabelnetz.dto.response.AuthResponse;
import com.vokabelnetz.entity.RefreshToken;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserPreferences;
import com.vokabelnetz.exception.AuthenticationException;
import com.vokabelnetz.exception.InvalidTokenException;
import com.vokabelnetz.exception.TokenReusedException;
import com.vokabelnetz.repository.RefreshTokenRepository;
import com.vokabelnetz.repository.UserPreferencesRepository;
import com.vokabelnetz.repository.UserRepository;
import com.vokabelnetz.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Authentication service.
 * Based on SECURITY.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;
    private final SecurityAlertService securityAlertService;

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        // Check if email exists - don't reveal this in response
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            // For security, we could send an email to the existing user
            // But for now, just throw a generic error
            log.warn("Registration attempted with existing email: {}", maskEmail(request.getEmail()));
            throw new AuthenticationException("Registration failed");
        }

        // Create user
        User user = User.builder()
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName())
            .timezone(request.getTimezone() != null ? request.getTimezone() : "Europe/Istanbul")
            .build();

        user = userRepository.save(user);

        // Create default preferences
        UserPreferences preferences = UserPreferences.builder()
            .user(user)
            .build();
        preferencesRepository.save(preferences);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user, httpRequest);

        // Send email verification (welcome email will be sent after verification)
        emailVerificationService.createAndSendVerificationToken(user, httpRequest);

        log.info("New user registered: {}", user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
            .user(mapToUserDto(user))
            .build();
    }

    /**
     * Authenticate user and generate tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase().trim();
        String ipAddress = getClientIp(httpRequest);

        // Check lockout
        if (!loginAttemptService.isLoginAllowed(email, ipAddress)) {
            // Don't reveal lockout - use same message as invalid credentials
            throw new AuthenticationException();
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElse(null);

        // Validate credentials
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailedAttempt(email, ipAddress);
            throw new AuthenticationException();
        }

        // Check if user is active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthenticationException("Account is disabled");
        }

        // Clear failed attempts
        loginAttemptService.recordSuccessfulLogin(email, ipAddress);

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user, httpRequest);

        // Enforce session limit
        enforceSessionLimit(user);

        log.info("User logged in: {}", user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
            .user(mapToUserDto(user))
            .build();
    }

    /**
     * Refresh tokens with rotation.
     * Old token is ALWAYS revoked, new token is ALWAYS generated.
     */
    @Transactional
    public AuthResponse refreshTokens(String oldRefreshToken, HttpServletRequest httpRequest) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(oldRefreshToken)
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Check if token was already revoked (potential theft!)
        if (Boolean.TRUE.equals(storedToken.getIsRevoked())) {
            log.warn("SECURITY: Refresh token reuse detected for user {}",
                storedToken.getUser().getId());

            // Send security alert
            securityAlertService.sendTokenReuseAlert(
                storedToken.getUser(),
                storedToken.getIpAddress(),
                storedToken.getUserAgent()
            );

            // Revoke ALL tokens for this user
            refreshTokenRepository.revokeAllByUserId(
                storedToken.getUser().getId(),
                Instant.now(),
                "TOKEN_REUSE_DETECTED"
            );

            throw new TokenReusedException("Token has been revoked. All sessions terminated.");
        }

        // Check expiration
        if (storedToken.isExpired()) {
            throw new InvalidTokenException("Refresh token expired");
        }

        // CRITICAL: Revoke old token immediately
        storedToken.setIsRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        storedToken.setRevokedReason("ROTATION");
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();

        // Generate new tokens
        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user, httpRequest);

        // Enforce session limit
        enforceSessionLimit(user);

        log.debug("Tokens refreshed for user: {}", user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(newRefreshToken)
            .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
            .user(mapToUserDto(user))
            .build();
    }

    /**
     * Logout - revoke refresh token.
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
            .ifPresent(token -> {
                token.setIsRevoked(true);
                token.setRevokedAt(Instant.now());
                token.setRevokedReason("LOGOUT");
                refreshTokenRepository.save(token);
                log.debug("User logged out: {}", token.getUser().getId());
            });
    }

    /**
     * Logout from all devices.
     */
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now(), "LOGOUT_ALL");
        log.info("User {} logged out from all devices", userId);
    }

    /**
     * Create a new refresh token.
     */
    private String createRefreshToken(User user, HttpServletRequest request) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(token)
            .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
            .ipAddress(getClientIp(request))
            .userAgent(request.getHeader("User-Agent"))
            .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    /**
     * Enforce maximum active sessions per user.
     */
    private void enforceSessionLimit(User user) {
        var config = appProperties.getSecurity();
        var activeTokens = refreshTokenRepository.findActiveByUserId(user.getId(), Instant.now());

        if (activeTokens.size() > config.getMaxActiveSessions()) {
            // Revoke oldest tokens
            activeTokens.stream()
                .skip(config.getMaxActiveSessions())
                .forEach(token -> {
                    token.setIsRevoked(true);
                    token.setRevokedAt(Instant.now());
                    token.setRevokedReason("SESSION_LIMIT");
                    refreshTokenRepository.save(token);
                });

            log.debug("Session limit enforced for user {}", user.getId());
        }
    }

    private AuthResponse.UserDto mapToUserDto(User user) {
        return AuthResponse.UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .role(user.getRole().name())
            .eloRating(user.getEloRating())
            .currentStreak(user.getCurrentStreak())
            .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "[INVALID]";
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) +
            "@" + domain.charAt(0) + "***" + domain.substring(domain.lastIndexOf('.'));
    }

    /**
     * Verify email with token.
     */
    @Transactional
    public void verifyEmail(String token) {
        User user = emailVerificationService.verifyEmail(token);

        // Send welcome email after successful verification
        emailService.sendWelcomeEmail(user);

        log.info("Email verified for user: {}", user.getId());
    }
}

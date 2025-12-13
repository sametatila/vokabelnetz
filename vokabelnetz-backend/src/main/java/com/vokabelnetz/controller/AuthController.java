package com.vokabelnetz.controller;

import com.vokabelnetz.dto.request.ForgotPasswordRequest;
import com.vokabelnetz.dto.request.LoginRequest;
import com.vokabelnetz.dto.request.RegisterRequest;
import com.vokabelnetz.dto.request.ResetPasswordRequest;
import com.vokabelnetz.dto.response.ApiResponse;
import com.vokabelnetz.dto.response.AuthResponse;
import com.vokabelnetz.dto.response.SessionResponse;
import com.vokabelnetz.entity.RefreshToken;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.security.CurrentUser;
import com.vokabelnetz.service.AuthService;
import com.vokabelnetz.service.EmailVerificationService;
import com.vokabelnetz.service.PasswordResetService;
import com.vokabelnetz.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Authentication controller.
 * Based on API.md and SECURITY.md documentation.
 *
 * Implements secure token management:
 * - Access token returned in response body (stored in memory by frontend)
 * - Refresh token set as HttpOnly cookie (not accessible via JavaScript)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SessionService sessionService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    /**
     * Register a new user.
     * POST /api/auth/register
     * Sets refresh token as HttpOnly cookie.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.register(request, httpRequest);
        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Login with email and password.
     * POST /api/auth/login
     * Sets refresh token as HttpOnly cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request, httpRequest);
        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh access token.
     * POST /api/auth/refresh
     * Reads refresh token from HttpOnly cookie.
     * Sets new refresh token as HttpOnly cookie (token rotation).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
        @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieToken,
        @RequestBody(required = false) Map<String, String> request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        // Prefer cookie token, fall back to body for backwards compatibility
        String refreshToken = cookieToken != null ? cookieToken :
            (request != null ? request.get("refreshToken") : null);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.vokabelnetz.exception.InvalidTokenException("No refresh token provided");
        }

        AuthResponse response = authService.refreshTokens(refreshToken, httpRequest);
        setRefreshTokenCookie(httpResponse, response.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Logout - revoke refresh token.
     * POST /api/auth/logout
     * Clears the refresh token cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
        @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String cookieToken,
        @RequestBody(required = false) Map<String, String> request,
        HttpServletResponse httpResponse
    ) {
        // Prefer cookie token, fall back to body for backwards compatibility
        String refreshToken = cookieToken != null ? cookieToken :
            (request != null ? request.get("refreshToken") : null);

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Logged out successfully")));
    }

    /**
     * Set refresh token as HttpOnly cookie.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Lax")  // Lax allows the cookie to be sent on top-level navigations
            .path("/api/auth")
            .maxAge(REFRESH_TOKEN_DURATION)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Clear refresh token cookie.
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(0)  // Immediate expiration
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Logout from all devices.
     * POST /api/auth/logout-all
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Map<String, String>>> logoutAll(
        @CurrentUser User user
    ) {
        authService.logoutAll(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Logged out from all devices")));
    }

    /**
     * Request password reset.
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        passwordResetService.requestPasswordReset(request.getEmail(), httpRequest);
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "If an account with that email exists, a password reset link has been sent.")
        ));
    }

    /**
     * Reset password with token.
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Password has been reset successfully. Please login with your new password.")
        ));
    }

    /**
     * Verify email address.
     * GET /api/auth/verify-email?token=xxx
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyEmail(
        @RequestParam String token
    ) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Email verified successfully")
        ));
    }

    /**
     * Resend email verification.
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Map<String, String>>> resendVerification(
        @CurrentUser User user,
        HttpServletRequest httpRequest
    ) {
        emailVerificationService.resendVerificationEmail(user, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Verification email sent")
        ));
    }

    /**
     * List active sessions.
     * GET /api/auth/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessions(
        @CurrentUser User user,
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String currentToken = extractToken(authHeader);
        List<SessionResponse> sessions = sessionService.getActiveSessions(user.getId(), currentToken);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "sessions", sessions,
            "totalSessions", sessions.size()
        )));
    }

    /**
     * Revoke specific session.
     * DELETE /api/auth/sessions/{id}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> revokeSession(
        @CurrentUser User user,
        @PathVariable Long sessionId
    ) {
        sessionService.revokeSession(user.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Session revoked successfully")
        ));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

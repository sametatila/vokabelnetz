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
import com.vokabelnetz.service.PasswordResetService;
import com.vokabelnetz.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Authentication controller.
 * Based on API.md and SECURITY.md documentation.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SessionService sessionService;

    /**
     * Register a new user.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Login with email and password.
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh access token.
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
        @RequestBody Map<String, String> request,
        HttpServletRequest httpRequest
    ) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = authService.refreshTokens(refreshToken, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Logout - revoke refresh token.
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
        @RequestBody Map<String, String> request
    ) {
        String refreshToken = request.get("refreshToken");
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Logged out successfully")));
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

package com.vokabelnetz.service;

import com.vokabelnetz.config.MailProperties;
import com.vokabelnetz.entity.PasswordResetToken;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.exception.BadRequestException;
import com.vokabelnetz.exception.InvalidTokenException;
import com.vokabelnetz.repository.PasswordResetTokenRepository;
import com.vokabelnetz.repository.RefreshTokenRepository;
import com.vokabelnetz.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Password reset service.
 * Based on SECURITY.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MailProperties mailProperties;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_EXPIRY_HOURS = 1;
    private static final int MAX_TOKENS_PER_HOUR = 3;

    /**
     * Request password reset.
     * Generates token and would normally send email.
     */
    @Transactional
    public void requestPasswordReset(String email, HttpServletRequest request) {
        String normalizedEmail = email.toLowerCase().trim();

        // Find user - don't reveal if user exists
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
            .orElse(null);

        if (user == null) {
            log.debug("Password reset requested for non-existent email: {}", maskEmail(email));
            return; // Silent fail to prevent enumeration
        }

        // Rate limiting - max 3 tokens per hour
        long recentTokens = tokenRepository.countByUserIdAndCreatedAtAfter(
            user.getId(),
            LocalDateTime.now().minusHours(1)
        );

        if (recentTokens >= MAX_TOKENS_PER_HOUR) {
            log.warn("Password reset rate limit exceeded for user: {}", user.getId());
            return; // Silent fail
        }

        // Invalidate any existing tokens
        tokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

        // Generate secure token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Store token hash (not the plain token)
        String tokenHash = hashToken(plainToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
            .ipAddress(getClientIp(request))
            .userAgent(request.getHeader("User-Agent"))
            .build();

        tokenRepository.save(resetToken);

        log.info("Password reset token generated for user: {}", user.getId());

        // Send password reset email
        emailService.sendPasswordResetEmail(user, plainToken);
    }

    /**
     * Reset password using token.
     */
    @Transactional
    public void resetPassword(String plainToken, String newPassword, HttpServletRequest request) {
        String tokenHash = hashToken(plainToken);

        PasswordResetToken resetToken = tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsedAt(Instant.now());
        tokenRepository.save(resetToken);

        // Revoke all refresh tokens (security: force re-login)
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(), "PASSWORD_RESET");

        // Send notification email
        emailService.sendPasswordChangedNotification(user);

        log.info("Password reset successful for user: {}", user.getId());
    }

    /**
     * Hash token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
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
}

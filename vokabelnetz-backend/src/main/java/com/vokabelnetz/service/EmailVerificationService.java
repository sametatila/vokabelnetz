package com.vokabelnetz.service;

import com.vokabelnetz.entity.EmailVerificationToken;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.exception.BadRequestException;
import com.vokabelnetz.exception.InvalidTokenException;
import com.vokabelnetz.exception.RateLimitException;
import com.vokabelnetz.repository.EmailVerificationTokenRepository;
import com.vokabelnetz.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Email verification service.
 * Based on SECURITY.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final int TOKEN_BYTES = 32;
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(7);
    private static final int MAX_TOKENS_PER_DAY = 5;

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Create and send email verification token.
     * Returns the raw token (to be sent via email); only hash is stored.
     */
    @Transactional
    public void createAndSendVerificationToken(User user, HttpServletRequest request) {
        // Rate limit: Max tokens per day per user
        long recentTokens = tokenRepository.countByUserIdAndCreatedAtAfter(
            user.getId(),
            Instant.now().minus(Duration.ofDays(1))
        );

        if (recentTokens >= MAX_TOKENS_PER_DAY) {
            log.warn("Email verification rate limit exceeded for user: {}", user.getId());
            throw new RateLimitException("Too many verification requests. Please try again later.");
        }

        // Invalidate existing tokens
        tokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

        // Generate secure random token
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        // Store HASH of token (not plain token)
        String tokenHash = hashToken(token);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .user(user)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plus(TOKEN_VALIDITY))
            .ipAddress(getClientIp(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .build();

        tokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendEmailVerificationEmail(user, token);

        log.info("Email verification token created for user: {}", user.getId());
    }

    /**
     * Resend verification email.
     */
    @Transactional
    public void resendVerificationEmail(User user, HttpServletRequest request) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email is already verified");
        }

        createAndSendVerificationToken(user, request);
    }

    /**
     * Verify email with token.
     */
    @Transactional
    public User verifyEmail(String token) {
        String tokenHash = hashToken(token);

        EmailVerificationToken verificationToken = tokenRepository
            .findByTokenHashAndUsedAtIsNull(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));

        // Check expiration
        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token has expired. Please request a new one.");
        }

        // Mark token as used
        verificationToken.setUsedAt(Instant.now());
        tokenRepository.save(verificationToken);

        // Mark user email as verified
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getId());

        return user;
    }

    /**
     * Check if user needs email verification.
     */
    public boolean needsVerification(User user) {
        return !Boolean.TRUE.equals(user.getEmailVerified());
    }

    /**
     * Hash token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Get client IP address.
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

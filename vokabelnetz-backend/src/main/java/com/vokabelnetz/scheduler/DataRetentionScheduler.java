package com.vokabelnetz.scheduler;

import com.vokabelnetz.repository.EmailVerificationTokenRepository;
import com.vokabelnetz.repository.PasswordResetTokenRepository;
import com.vokabelnetz.repository.RefreshTokenRepository;
import com.vokabelnetz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled tasks for data retention and cleanup.
 * Based on SECURITY.md and GDPR requirements.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataRetentionScheduler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    /**
     * Permanently delete soft-deleted users after 30 days.
     * GDPR compliance: Users can request deletion, data is kept 30 days for recovery.
     * Runs daily at 03:00.
     */
    @Scheduled(cron = "0 0 3 * * *") // 03:00 every day
    @Transactional
    public void permanentlyDeleteExpiredUsers() {
        log.info("Starting expired user deletion job...");

        LocalDateTime cutoffDate = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        int deleted = userRepository.deleteByDeletedAtBefore(cutoffDate);

        log.info("Permanently deleted {} users (soft-deleted more than 30 days ago)", deleted);
    }

    /**
     * Clean up expired refresh tokens.
     * Runs daily at 04:00.
     */
    @Scheduled(cron = "0 0 4 * * *") // 04:00 every day
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        log.info("Starting refresh token cleanup job...");

        Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteExpiredTokens(cutoffDate);

        log.info("Deleted {} expired refresh tokens", deleted);
    }

    /**
     * Clean up expired and used password reset tokens.
     * Runs daily at 04:30.
     */
    @Scheduled(cron = "0 30 4 * * *") // 04:30 every day
    @Transactional
    public void cleanupPasswordResetTokens() {
        log.info("Starting password reset token cleanup job...");

        Instant cutoffDate = Instant.now().minus(24, ChronoUnit.HOURS);
        int deleted = passwordResetTokenRepository.deleteExpiredTokens(cutoffDate);

        log.info("Deleted {} expired password reset tokens", deleted);
    }

    /**
     * Clean up expired and used email verification tokens.
     * Runs daily at 04:45.
     */
    @Scheduled(cron = "0 45 4 * * *") // 04:45 every day
    @Transactional
    public void cleanupEmailVerificationTokens() {
        log.info("Starting email verification token cleanup job...");

        Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = emailVerificationTokenRepository.deleteExpiredTokens(cutoffDate);

        log.info("Deleted {} expired email verification tokens", deleted);
    }

    /**
     * Clean up revoked refresh tokens older than 30 days.
     * Keeps some history for security auditing.
     * Runs weekly on Sunday at 05:00.
     */
    @Scheduled(cron = "0 0 5 * * SUN") // 05:00 every Sunday
    @Transactional
    public void cleanupOldRevokedTokens() {
        log.info("Starting old revoked token cleanup job...");

        Instant cutoffDate = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffDate);

        log.info("Deleted {} old revoked refresh tokens", deleted);
    }
}

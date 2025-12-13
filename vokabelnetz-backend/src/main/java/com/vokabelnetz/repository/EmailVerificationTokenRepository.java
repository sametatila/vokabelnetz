package com.vokabelnetz.repository;

import com.vokabelnetz.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find valid token by hash.
     */
    Optional<EmailVerificationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    /**
     * Check if user has any valid (unused, unexpired) verification token.
     */
    @Query("""
        SELECT CASE WHEN COUNT(evt) > 0 THEN true ELSE false END
        FROM EmailVerificationToken evt
        WHERE evt.user.id = :userId
        AND evt.usedAt IS NULL
        AND evt.expiresAt > :now
        """)
    boolean existsValidTokenByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Invalidate all existing tokens for user.
     */
    @Modifying
    @Query("""
        UPDATE EmailVerificationToken evt
        SET evt.usedAt = :now
        WHERE evt.user.id = :userId
        AND evt.usedAt IS NULL
        """)
    void invalidateAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Delete expired tokens (for cleanup scheduler).
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :cutoffDate OR evt.usedAt IS NOT NULL")
    int deleteExpiredTokens(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count pending verification tokens for user (for rate limiting).
     */
    @Query("""
        SELECT COUNT(evt) FROM EmailVerificationToken evt
        WHERE evt.user.id = :userId
        AND evt.createdAt > :after
        """)
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("after") LocalDateTime after);
}

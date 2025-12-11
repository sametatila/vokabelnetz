package com.vokabelnetz.repository;

import com.vokabelnetz.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find valid token by hash.
     */
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    /**
     * Count recent tokens for rate limiting.
     */
    @Query("""
        SELECT COUNT(prt) FROM PasswordResetToken prt
        WHERE prt.user.id = :userId
        AND prt.createdAt > :after
        """)
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("after") LocalDateTime after);

    /**
     * Invalidate all existing tokens for user.
     */
    @Modifying
    @Query("""
        UPDATE PasswordResetToken prt
        SET prt.usedAt = :now
        WHERE prt.user.id = :userId
        AND prt.usedAt IS NULL
        """)
    void invalidateAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Delete expired tokens older than given timestamp.
     * For cleanup job.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}

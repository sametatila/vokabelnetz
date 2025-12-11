package com.vokabelnetz.repository;

import com.vokabelnetz.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Find active (non-revoked, non-expired) tokens for user.
     */
    @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.user.id = :userId
        AND rt.isRevoked = false
        AND rt.expiresAt > :now
        ORDER BY rt.createdAt DESC
        """)
    List<RefreshToken> findActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Revoke all tokens for a user.
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.isRevoked = true, rt.revokedAt = :now, rt.revokedReason = :reason
        WHERE rt.user.id = :userId AND rt.isRevoked = false
        """)
    void revokeAllByUserId(
        @Param("userId") Long userId,
        @Param("now") Instant now,
        @Param("reason") String reason
    );

    /**
     * Delete expired tokens older than given timestamp.
     * For cleanup job.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);

    /**
     * Delete revoked tokens older than given timestamp.
     * For cleanup job.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.isRevoked = true AND rt.revokedAt < :before")
    int deleteRevokedBefore(@Param("before") Instant before);

    /**
     * Count active sessions for user.
     */
    @Query("""
        SELECT COUNT(rt) FROM RefreshToken rt
        WHERE rt.user.id = :userId
        AND rt.isRevoked = false
        AND rt.expiresAt > :now
        """)
    long countActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}

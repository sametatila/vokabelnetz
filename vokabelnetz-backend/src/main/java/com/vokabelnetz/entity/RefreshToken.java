package com.vokabelnetz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Refresh token entity for JWT authentication.
 * Implements token rotation and reuse detection as per SECURITY.md.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_rt_user_id", columnList = "user_id"),
    @Index(name = "idx_rt_token", columnList = "token"),
    @Index(name = "idx_rt_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_revoked")
    private Boolean isRevoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 50)
    private String revokedReason;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_info", length = 200)
    private String deviceInfo;

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Check if token is valid (not revoked and not expired).
     */
    public boolean isValid() {
        return !Boolean.TRUE.equals(isRevoked) && !isExpired();
    }
}

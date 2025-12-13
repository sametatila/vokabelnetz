package com.vokabelnetz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Email verification token entity.
 * Token is stored as hash for security (SECURITY.md).
 */
@Entity
@Table(name = "email_verification_tokens", indexes = {
    @Index(name = "idx_evt_user_id", columnList = "user_id"),
    @Index(name = "idx_evt_token_hash", columnList = "token_hash"),
    @Index(name = "idx_evt_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Check if token has been used.
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Check if token is valid (not used and not expired).
     */
    public boolean isValid() {
        return !isUsed() && !isExpired();
    }
}

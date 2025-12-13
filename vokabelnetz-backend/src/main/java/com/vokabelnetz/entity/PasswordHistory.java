package com.vokabelnetz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity for storing password history to prevent password reuse.
 * Based on SECURITY.md documentation.
 */
@Entity
@Table(name = "password_history", indexes = {
    @Index(name = "idx_password_history_user_id", columnList = "user_id"),
    @Index(name = "idx_password_history_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}

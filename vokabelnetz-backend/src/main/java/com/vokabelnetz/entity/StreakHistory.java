package com.vokabelnetz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Tracks streak history for analytics and recovery.
 */
@Entity
@Table(name = "streak_history", indexes = {
    @Index(name = "idx_sh_user_id", columnList = "user_id"),
    @Index(name = "idx_sh_date", columnList = "streak_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreakHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "streak_date", nullable = false)
    private LocalDate streakDate;

    @Column(name = "streak_count", nullable = false)
    private Integer streakCount;

    @Builder.Default
    @Column(name = "was_active")
    private Boolean wasActive = false;

    @Builder.Default
    @Column(name = "freeze_used")
    private Boolean freezeUsed = false;
}

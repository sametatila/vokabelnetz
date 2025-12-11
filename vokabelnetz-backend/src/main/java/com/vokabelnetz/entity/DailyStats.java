package com.vokabelnetz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Daily statistics for user activity tracking.
 */
@Entity
@Table(name = "daily_stats", indexes = {
    @Index(name = "idx_ds_user_id", columnList = "user_id"),
    @Index(name = "idx_ds_stat_date", columnList = "stat_date")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ds_user_date", columnNames = {"user_id", "stat_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStats extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Builder.Default
    @Column(name = "words_reviewed")
    private Integer wordsReviewed = 0;

    @Builder.Default
    @Column(name = "words_correct")
    private Integer wordsCorrect = 0;

    @Builder.Default
    @Column(name = "new_words_learned")
    private Integer newWordsLearned = 0;

    @Builder.Default
    @Column(name = "sessions_completed")
    private Integer sessionsCompleted = 0;

    @Builder.Default
    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds = 0;

    @Builder.Default
    @Column(name = "streak_maintained")
    private Boolean streakMaintained = false;

    @Builder.Default
    @Column(name = "freeze_used")
    private Boolean freezeUsed = false;

    @Builder.Default
    @Column(name = "xp_earned")
    private Integer xpEarned = 0;

    /**
     * Calculate daily accuracy.
     */
    public double getAccuracy() {
        if (wordsReviewed == null || wordsReviewed == 0) {
            return 0.0;
        }
        return (double) (wordsCorrect != null ? wordsCorrect : 0) / wordsReviewed;
    }
}

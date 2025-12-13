package com.vokabelnetz.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks user's progress on individual words using SM-2 algorithm.
 */
@Entity
@Table(name = "user_word_progress", indexes = {
    @Index(name = "idx_uwp_user_id", columnList = "user_id"),
    @Index(name = "idx_uwp_next_review", columnList = "next_review_at"),
    @Index(name = "idx_uwp_user_next_review", columnList = "user_id, next_review_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_uwp_user_word", columnNames = {"user_id", "word_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWordProgress extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    // SM-2 Algorithm Variables
    @Builder.Default
    @Column(name = "ease_factor", precision = 4)
    private Double easeFactor = 2.5;

    @Builder.Default
    @Column(name = "interval_days")
    private Integer intervalDays = 1;

    @Builder.Default
    private Integer repetition = 0;

    @Column(name = "last_quality")
    private Integer lastQuality;

    // Review scheduling
    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    // Performance tracking
    @Builder.Default
    @Column(name = "times_correct")
    private Integer timesCorrect = 0;

    @Builder.Default
    @Column(name = "times_incorrect")
    private Integer timesIncorrect = 0;

    @Column(name = "last_response_time_ms")
    private Integer lastResponseTimeMs;

    @Column(name = "avg_response_time_ms")
    private Integer avgResponseTimeMs;

    // Learning status
    @Builder.Default
    @Column(name = "is_learned")
    private Boolean isLearned = false;

    @Column(name = "learned_at")
    private LocalDateTime learnedAt;

    @Builder.Default
    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

    @Builder.Default
    @Column(name = "is_difficult")
    private Boolean isDifficult = false;

    /**
     * Calculate success rate for this word.
     */
    public double getSuccessRate() {
        int total = (timesCorrect != null ? timesCorrect : 0) + (timesIncorrect != null ? timesIncorrect : 0);
        if (total == 0) {
            return 0.0;
        }
        return (double) (timesCorrect != null ? timesCorrect : 0) / total;
    }

    /**
     * Check if word is due for review.
     */
    public boolean isDueForReview() {
        return nextReviewAt != null && nextReviewAt.isBefore(LocalDateTime.now());
    }
}

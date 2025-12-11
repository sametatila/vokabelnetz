package com.vokabelnetz.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.SessionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks individual learning sessions.
 */
@Entity
@Table(name = "learning_sessions", indexes = {
    @Index(name = "idx_ls_user_id", columnList = "user_id"),
    @Index(name = "idx_ls_started_at", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningSession extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", length = 2)
    private CefrLevel cefrLevel;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder.Default
    @Column(name = "words_reviewed")
    private Integer wordsReviewed = 0;

    @Builder.Default
    @Column(name = "words_correct")
    private Integer wordsCorrect = 0;

    @Builder.Default
    @Column(name = "new_words_learned")
    private Integer newWordsLearned = 0;

    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds;

    @Column(name = "avg_response_time_ms")
    private Integer avgResponseTimeMs;

    /**
     * Calculate accuracy for this session.
     */
    public double getAccuracy() {
        if (wordsReviewed == null || wordsReviewed == 0) {
            return 0.0;
        }
        return (double) (wordsCorrect != null ? wordsCorrect : 0) / wordsReviewed;
    }

    /**
     * Check if session is active.
     */
    public boolean isActive() {
        return endedAt == null;
    }
}

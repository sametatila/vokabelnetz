package com.vokabelnetz.dto.response;

import com.vokabelnetz.service.StreakService.StreakStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of processing an answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResult {

    private boolean correct;
    private int quality;

    // Elo update info
    private int eloChange;
    private int newUserRating;
    private int newWordRating;
    private double expectedScore;

    // SM-2 update info
    private double newEaseFactor;
    private int newInterval;
    private LocalDateTime nextReviewAt;
    private Boolean isLearned;

    // Streak info
    private StreakStatus streakStatus;
}

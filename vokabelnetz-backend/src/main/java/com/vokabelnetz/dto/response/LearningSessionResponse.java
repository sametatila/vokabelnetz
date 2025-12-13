package com.vokabelnetz.dto.response;

import com.vokabelnetz.entity.LearningSession;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.SessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for learning session.
 * Maps entity fields to API.md specification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSessionResponse {
    private Long sessionId;
    private SessionType sessionType;
    private CefrLevel cefrLevel;
    private Integer wordsToReview;
    private Integer newWordsAvailable;
    private LocalDateTime startedAt;

    /**
     * Create response from entity.
     */
    public static LearningSessionResponse from(LearningSession session) {
        return LearningSessionResponse.builder()
            .sessionId(session.getId())
            .sessionType(session.getSessionType())
            .cefrLevel(session.getCefrLevel())
            .wordsToReview(session.getWordsReviewed() != null ? session.getWordsReviewed() : 0)
            .newWordsAvailable(session.getNewWordsLearned() != null ? session.getNewWordsLearned() : 0)
            .startedAt(session.getStartedAt())
            .build();
    }

    /**
     * Create response with custom word counts.
     */
    public static LearningSessionResponse from(LearningSession session, int wordsToReview, int newWordsAvailable) {
        return LearningSessionResponse.builder()
            .sessionId(session.getId())
            .sessionType(session.getSessionType())
            .cefrLevel(session.getCefrLevel())
            .wordsToReview(wordsToReview)
            .newWordsAvailable(newWordsAvailable)
            .startedAt(session.getStartedAt())
            .build();
    }
}

package com.vokabelnetz.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for submitting an answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {

    @NotNull(message = "wordId is required")
    private Long wordId;

    @NotNull(message = "correct is required")
    private Boolean correct;

    /**
     * Whether the user recognized the answer after seeing it (for incorrect answers).
     */
    private boolean recognized;

    /**
     * Whether the user used a hint.
     */
    private boolean usedHint;

    /**
     * Response time in milliseconds.
     */
    @Min(value = 0, message = "responseTimeMs must be positive")
    private int responseTimeMs;

    /**
     * Session ID if within a learning session.
     */
    private Long sessionId;

    public boolean isCorrect() {
        return Boolean.TRUE.equals(correct);
    }

    public boolean isRecognized() {
        return recognized;
    }

    public boolean isUsedHint() {
        return usedHint;
    }
}

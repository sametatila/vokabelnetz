package com.vokabelnetz.controller;

import com.vokabelnetz.dto.request.AnswerRequest;
import com.vokabelnetz.dto.request.StartSessionRequest;
import com.vokabelnetz.dto.response.AnswerResult;
import com.vokabelnetz.dto.response.ApiResponse;
import com.vokabelnetz.dto.response.NextWordResult;
import com.vokabelnetz.entity.LearningSession;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserWordProgress;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.SessionType;
import com.vokabelnetz.security.CurrentUser;
import com.vokabelnetz.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Learning endpoints controller.
 * Based on API.md documentation.
 */
@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    /**
     * Start a learning session.
     * POST /api/learning/session/start
     */
    @PostMapping("/session/start")
    public ResponseEntity<ApiResponse<LearningSession>> startSession(
        @CurrentUser User user,
        @RequestBody(required = false) StartSessionRequest request
    ) {
        SessionType type = request != null && request.getSessionType() != null
            ? request.getSessionType() : SessionType.MIXED;
        CefrLevel level = request != null && request.getCefrLevel() != null
            ? request.getCefrLevel() : CefrLevel.A1;
        Integer wordCount = request != null && request.getWordCount() != null
            ? request.getWordCount() : 20;

        LearningSession session = learningService.startSession(user, type, level);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    /**
     * Get current active session.
     * GET /api/learning/session/current
     */
    @GetMapping("/session/current")
    public ResponseEntity<ApiResponse<LearningSession>> getCurrentSession(
        @CurrentUser User user
    ) {
        LearningSession session = learningService.getCurrentSession(user);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    /**
     * End learning session.
     * POST /api/learning/session/end
     */
    @PostMapping("/session/end")
    public ResponseEntity<ApiResponse<Map<String, Object>>> endSession(
        @CurrentUser User user,
        @RequestBody Map<String, Long> request
    ) {
        Long sessionId = request.get("sessionId");
        LearningSession session = learningService.endSession(sessionId);

        // Return session summary
        int reviewed = session.getWordsReviewed() != null ? session.getWordsReviewed() : 0;
        int correct = session.getWordsCorrect() != null ? session.getWordsCorrect() : 0;
        Map<String, Object> summary = Map.of(
            "sessionId", session.getId(),
            "duration", session.getTotalTimeSeconds() != null ? session.getTotalTimeSeconds() : 0,
            "wordsReviewed", reviewed,
            "wordsNew", session.getNewWordsLearned() != null ? session.getNewWordsLearned() : 0,
            "correctAnswers", correct,
            "incorrectAnswers", reviewed - correct
        );

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get next word for learning.
     * GET /api/learning/next
     */
    @GetMapping("/next")
    public ResponseEntity<ApiResponse<NextWordResult>> getNextWord(
        @CurrentUser User user,
        @RequestParam(required = false) Long sessionId,
        @RequestParam(defaultValue = "A1") CefrLevel level
    ) {
        NextWordResult result = learningService.getNextWord(user, level);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Submit an answer.
     * POST /api/learning/answer
     */
    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<AnswerResult>> submitAnswer(
        @CurrentUser User user,
        @Valid @RequestBody AnswerRequest request
    ) {
        AnswerResult result = learningService.processAnswer(user, request.getWordId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get words due for review.
     * GET /api/learning/review
     */
    @GetMapping("/review")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewWords(
        @CurrentUser User user,
        @RequestParam(defaultValue = "20") int limit
    ) {
        List<UserWordProgress> dueWords = learningService.getWordsForReview(user, limit);
        int totalDue = learningService.getReviewCount(user);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "totalDue", totalDue,
            "words", dueWords,
            "overdueCount", dueWords.stream()
                .filter(w -> w.getNextReviewAt() != null &&
                    w.getNextReviewAt().isBefore(java.time.LocalDateTime.now()))
                .count(),
            "dueTodayCount", dueWords.size()
        )));
    }

    /**
     * Get review count.
     * GET /api/learning/review/count
     */
    @GetMapping("/review/count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getReviewCount(
        @CurrentUser User user
    ) {
        int count = learningService.getReviewCount(user);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "dueCount", count
        )));
    }

    /**
     * Get new words available for learning.
     * GET /api/learning/new
     */
    @GetMapping("/new")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNewWords(
        @CurrentUser User user,
        @RequestParam(defaultValue = "A1") CefrLevel level,
        @RequestParam(defaultValue = "20") int limit
    ) {
        var newWords = learningService.getNewWords(user, level, limit);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "words", newWords,
            "count", newWords.size(),
            "level", level.name()
        )));
    }

    /**
     * Get quiz words (mixed review and new).
     * GET /api/learning/quiz
     */
    @GetMapping("/quiz")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizWords(
        @CurrentUser User user,
        @RequestParam(defaultValue = "A1") CefrLevel level,
        @RequestParam(defaultValue = "10") int count
    ) {
        var quizWords = learningService.getQuizWords(user, level, count);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "words", quizWords,
            "count", quizWords.size()
        )));
    }

}

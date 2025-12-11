package com.vokabelnetz.service;

import com.vokabelnetz.algorithm.EloRatingService;
import com.vokabelnetz.algorithm.SpacedRepetitionService;
import com.vokabelnetz.dto.request.AnswerRequest;
import com.vokabelnetz.dto.response.AnswerResult;
import com.vokabelnetz.dto.response.NextWordResult;
import com.vokabelnetz.entity.LearningSession;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserWordProgress;
import com.vokabelnetz.entity.Word;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.SessionType;
import com.vokabelnetz.exception.ResourceNotFoundException;
import com.vokabelnetz.repository.LearningSessionRepository;
import com.vokabelnetz.repository.UserWordProgressRepository;
import com.vokabelnetz.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Main learning service that orchestrates SM-2, Elo, and Streak systems.
 * Based on ALGORITHMS.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LearningService {

    private final SpacedRepetitionService sm2Service;
    private final EloRatingService eloService;
    private final StreakService streakService;
    private final DailyStatsService statsService;
    private final WordRepository wordRepository;
    private final UserWordProgressRepository progressRepository;
    private final LearningSessionRepository sessionRepository;

    /**
     * Process a user's answer to a word.
     */
    @Transactional
    public AnswerResult processAnswer(User user, Long wordId, AnswerRequest request) {
        Word word = wordRepository.findById(wordId)
            .orElseThrow(() -> new ResourceNotFoundException("Word", wordId));

        // Get or create progress
        UserWordProgress progress = progressRepository.findByUserIdAndWordId(user.getId(), wordId)
            .orElseGet(() -> {
                UserWordProgress newProgress = UserWordProgress.builder()
                    .user(user)
                    .word(word)
                    .build();
                return sm2Service.initializeProgress(newProgress);
            });

        boolean correct = request.isCorrect();
        int quality = mapToQuality(request);

        // 1. Update Elo ratings
        var eloResult = eloService.updateRatings(user, word, correct);

        // 2. Update SM-2 scheduling
        progress = sm2Service.calculateNextReview(progress, quality);
        progress.setTimesCorrect(progress.getTimesCorrect() + (correct ? 1 : 0));
        progress.setTimesIncorrect(progress.getTimesIncorrect() + (correct ? 0 : 1));
        progress.setLastResponseTimeMs(request.getResponseTimeMs());

        // Update average response time
        int totalResponses = progress.getTimesCorrect() + progress.getTimesIncorrect();
        if (progress.getAvgResponseTimeMs() != null && totalResponses > 1) {
            int avg = ((progress.getAvgResponseTimeMs() * (totalResponses - 1))
                + request.getResponseTimeMs()) / totalResponses;
            progress.setAvgResponseTimeMs(avg);
        } else {
            progress.setAvgResponseTimeMs(request.getResponseTimeMs());
        }

        progressRepository.save(progress);

        // 3. Update daily stats
        statsService.recordAnswer(user.getId(), correct, request.getResponseTimeMs());

        // 4. Update word global stats
        word.setTimesShown(word.getTimesShown() + 1);
        if (correct) {
            word.setTimesCorrect(word.getTimesCorrect() + 1);
        }
        wordRepository.save(word);

        // 5. Get streak status
        var streakStatus = streakService.getStreakStatus(user);

        log.debug("Answer processed: user={}, word={}, correct={}, quality={}, newInterval={}",
            user.getId(), wordId, correct, quality, progress.getIntervalDays());

        return AnswerResult.builder()
            .correct(correct)
            .quality(quality)
            .eloChange(eloResult.userChange())
            .newUserRating(eloResult.newUserRating())
            .newWordRating(eloResult.newWordRating())
            .expectedScore(eloResult.expectedScore())
            .newEaseFactor(progress.getEaseFactor())
            .newInterval(progress.getIntervalDays())
            .nextReviewAt(progress.getNextReviewAt())
            .isLearned(progress.getIsLearned())
            .streakStatus(streakStatus)
            .build();
    }

    /**
     * Get next word for learning.
     */
    public NextWordResult getNextWord(User user, CefrLevel cefrLevel) {
        // Priority 1: Due review words
        List<UserWordProgress> dueWords = sm2Service.getWordsForReview(user.getId(), 10);

        if (!dueWords.isEmpty()) {
            List<Word> words = dueWords.stream()
                .map(p -> wordRepository.findById(p.getWord().getId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();

            Word selected = eloService.selectNextWord(user, words);
            if (selected != null) {
                UserWordProgress progress = dueWords.stream()
                    .filter(p -> p.getWord().getId().equals(selected.getId()))
                    .findFirst()
                    .orElse(null);

                return NextWordResult.builder()
                    .word(selected)
                    .progress(progress)
                    .isReview(true)
                    .dueCount(dueWords.size())
                    .build();
            }
        }

        // Priority 2: New words
        List<Word> newWords = wordRepository.findNewWordsForUser(
            user.getId(),
            cefrLevel,
            PageRequest.of(0, 20)
        );

        if (!newWords.isEmpty()) {
            Word selected = eloService.selectNextWord(user, newWords);
            return NextWordResult.builder()
                .word(selected)
                .progress(null)
                .isReview(false)
                .dueCount(0)
                .build();
        }

        // No words available
        return NextWordResult.builder()
            .word(null)
            .progress(null)
            .isReview(false)
            .dueCount(0)
            .build();
    }

    /**
     * Start a new learning session.
     */
    @Transactional
    public LearningSession startSession(User user, SessionType sessionType, CefrLevel cefrLevel) {
        // End any existing active session
        sessionRepository.findByUserIdAndEndedAtIsNull(user.getId())
            .ifPresent(session -> {
                session.setEndedAt(LocalDateTime.now());
                sessionRepository.save(session);
            });

        LearningSession session = LearningSession.builder()
            .user(user)
            .sessionType(sessionType)
            .cefrLevel(cefrLevel)
            .startedAt(LocalDateTime.now())
            .build();

        return sessionRepository.save(session);
    }

    /**
     * End a learning session.
     */
    @Transactional
    public LearningSession endSession(Long sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("LearningSession", sessionId));

        if (session.getEndedAt() == null) {
            session.setEndedAt(LocalDateTime.now());

            // Calculate duration
            long seconds = java.time.Duration.between(
                session.getStartedAt(),
                session.getEndedAt()
            ).getSeconds();
            session.setTotalTimeSeconds((int) seconds);

            sessionRepository.save(session);

            // Record session completion
            statsService.recordSessionCompleted(session.getUser().getId(), (int) seconds);
        }

        return session;
    }

    /**
     * Get current active session for user.
     */
    public LearningSession getCurrentSession(User user) {
        return sessionRepository.findByUserIdAndEndedAtIsNull(user.getId())
            .orElse(null);
    }

    /**
     * Get words due for review.
     */
    public List<UserWordProgress> getWordsForReview(User user, int limit) {
        return sm2Service.getWordsForReview(user.getId(), limit);
    }

    /**
     * Get count of words due for review.
     */
    public int getReviewCount(User user) {
        return progressRepository.countOverdue(user.getId(), LocalDateTime.now());
    }

    /**
     * Get new words for a user (words they haven't learned yet).
     */
    public List<Word> getNewWords(User user, CefrLevel level, int limit) {
        return wordRepository.findNewWordsForUser(
            user.getId(),
            level,
            PageRequest.of(0, limit)
        );
    }

    /**
     * Get quiz words (mixed review and new).
     */
    public List<Word> getQuizWords(User user, CefrLevel level, int count) {
        // Get some due words and some new words
        int dueCount = count / 2;
        int newCount = count - dueCount;

        List<UserWordProgress> dueWords = sm2Service.getWordsForReview(user.getId(), dueCount);
        List<Word> result = new java.util.ArrayList<>(
            dueWords.stream()
                .map(p -> wordRepository.findById(p.getWord().getId()).orElse(null))
                .filter(Objects::nonNull)
                .toList()
        );

        // Add new words if needed
        if (result.size() < count) {
            List<Word> newWords = wordRepository.findNewWordsForUser(
                user.getId(),
                level,
                PageRequest.of(0, count - result.size())
            );
            result.addAll(newWords);
        }

        return result;
    }

    /**
     * Map user response to SM-2 quality score (0-5).
     */
    private int mapToQuality(AnswerRequest request) {
        if (!request.isCorrect()) {
            return request.isRecognized() ? 1 : 0;
        }

        if (request.isUsedHint()) {
            return 2;
        }

        // Map response time to quality
        int responseMs = request.getResponseTimeMs();
        if (responseMs < 2000) return 5;      // < 2s = perfect
        if (responseMs < 5000) return 4;      // 2-5s = good
        return 3;                              // > 5s = difficult
    }
}

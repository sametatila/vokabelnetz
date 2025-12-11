package com.vokabelnetz.algorithm;

import com.vokabelnetz.config.AppProperties;
import com.vokabelnetz.entity.UserWordProgress;
import com.vokabelnetz.repository.UserWordProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SM-2 Spaced Repetition Algorithm implementation.
 * Based on ALGORITHMS.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SpacedRepetitionService {

    private final UserWordProgressRepository progressRepository;
    private final AppProperties appProperties;

    /**
     * Calculate next review based on SM-2 algorithm.
     *
     * @param progress Current user progress on word
     * @param quality  Quality of response (0-5)
     * @return Updated progress with new interval and next review date
     */
    @Transactional
    public UserWordProgress calculateNextReview(UserWordProgress progress, int quality) {
        var algorithmConfig = appProperties.getAlgorithm();

        // Validate quality (0-5 range)
        quality = Math.max(0, Math.min(5, quality));

        // If quality < 3, reset repetitions (incorrect answer)
        if (quality < 3) {
            progress.setRepetition(0);
            progress.setIntervalDays(1);
        } else {
            // Correct answer - calculate new interval
            int repetition = progress.getRepetition();

            if (repetition == 0) {
                progress.setIntervalDays(1);
            } else if (repetition == 1) {
                progress.setIntervalDays(6);
            } else {
                int newInterval = (int) Math.round(
                    progress.getIntervalDays() * progress.getEaseFactor()
                );
                // Cap maximum interval
                progress.setIntervalDays(Math.min(newInterval, algorithmConfig.getMaxInterval()));
            }

            progress.setRepetition(repetition + 1);
        }

        // Update ease factor using SM-2 formula
        double ef = progress.getEaseFactor();
        double newEf = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        newEf = Math.max(algorithmConfig.getMinEaseFactor(),
            Math.min(algorithmConfig.getMaxEaseFactor(), newEf));
        progress.setEaseFactor(newEf);

        // Set next review date
        LocalDateTime nextReview = LocalDateTime.now()
            .plusDays(progress.getIntervalDays());
        progress.setNextReviewAt(nextReview);
        progress.setLastQuality(quality);
        progress.setLastReviewedAt(LocalDateTime.now());

        // Check if word is "learned" (interval > threshold)
        if (progress.getIntervalDays() > algorithmConfig.getLearnedThresholdDays()
            && !Boolean.TRUE.equals(progress.getIsLearned())) {
            progress.setIsLearned(true);
            progress.setLearnedAt(LocalDateTime.now());
            log.debug("Word marked as learned for user {} and word {}",
                progress.getUser().getId(), progress.getWord().getId());
        }

        return progress;
    }

    /**
     * Get words due for review.
     */
    public List<UserWordProgress> getWordsForReview(Long userId, int limit) {
        return progressRepository.findDueForReview(
            userId,
            LocalDateTime.now(),
            PageRequest.of(0, limit, Sort.by("nextReviewAt").ascending())
        );
    }

    /**
     * Get count of overdue words.
     */
    public int getOverdueCount(Long userId) {
        return progressRepository.countOverdue(userId, LocalDateTime.now());
    }

    /**
     * Initialize progress for a new word.
     */
    public UserWordProgress initializeProgress(UserWordProgress progress) {
        var algorithmConfig = appProperties.getAlgorithm();

        progress.setEaseFactor(algorithmConfig.getDefaultEaseFactor());
        progress.setIntervalDays(1);
        progress.setRepetition(0);
        progress.setNextReviewAt(LocalDateTime.now());
        progress.setTimesCorrect(0);
        progress.setTimesIncorrect(0);
        progress.setIsLearned(false);
        progress.setIsFavorite(false);
        progress.setIsDifficult(false);

        return progress;
    }
}

package com.vokabelnetz.algorithm;

import com.vokabelnetz.config.AppProperties;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.Word;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Elo Rating System implementation for difficulty matching.
 * Based on ALGORITHMS.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EloRatingService {

    private final AppProperties appProperties;
    private final Random random = new Random();

    /**
     * Calculate expected probability of correct answer.
     */
    public double calculateExpectedScore(int userRating, int wordDifficulty) {
        return 1.0 / (1 + Math.pow(10, (wordDifficulty - userRating) / 400.0));
    }

    /**
     * Update both user and word ratings based on answer.
     */
    @Transactional
    public EloUpdateResult updateRatings(User user, Word word, boolean correct) {
        var config = appProperties.getAlgorithm();

        int userRating = user.getEloRating();
        int wordRating = word.getDifficultyRating();

        // Calculate expected score
        double expected = calculateExpectedScore(userRating, wordRating);
        int actual = correct ? 1 : 0;

        // Calculate rating changes
        int userChange = (int) Math.round(config.getKFactor() * (actual - expected));
        int wordChange = (int) Math.round(config.getKFactor() * (expected - actual));

        // Apply changes with bounds
        int newUserRating = clamp(userRating + userChange, config.getMinRating(), config.getMaxRating());
        int newWordRating = clamp(wordRating + wordChange, config.getMinRating(), config.getMaxRating());

        // Update entities
        user.setEloRating(newUserRating);
        word.setDifficultyRating(newWordRating);

        log.debug("Elo update: user {} -> {}, word {} -> {}",
            userRating, newUserRating, wordRating, newWordRating);

        return new EloUpdateResult(
            userRating, newUserRating, userChange,
            wordRating, newWordRating, wordChange,
            expected
        );
    }

    /**
     * Select optimal word for user based on Elo matching.
     * Words within ±tolerance rating points are considered optimal.
     */
    public Word selectNextWord(User user, List<Word> availableWords) {
        if (availableWords == null || availableWords.isEmpty()) {
            return null;
        }

        var config = appProperties.getAlgorithm();
        int userRating = user.getEloRating();
        int tolerance = config.getMatchTolerance();

        // Filter words within skill range
        List<Word> matchedWords = availableWords.stream()
            .filter(w -> Math.abs(w.getDifficultyRating() - userRating) <= tolerance)
            .toList();

        if (matchedWords.isEmpty()) {
            // Fall back to closest word if no matches
            return availableWords.stream()
                .min(Comparator.comparingInt(w ->
                    Math.abs(w.getDifficultyRating() - userRating)))
                .orElse(null);
        }

        // Weighted random selection (prefer closer matches)
        return weightedRandomSelect(matchedWords, userRating);
    }

    /**
     * Weighted random selection - closer matches have higher probability.
     */
    private Word weightedRandomSelect(List<Word> words, int userRating) {
        if (words.size() == 1) {
            return words.getFirst();
        }

        // Calculate weights (closer = higher weight)
        double[] weights = words.stream()
            .mapToDouble(w -> 1.0 / (1 + Math.abs(w.getDifficultyRating() - userRating)))
            .toArray();

        double totalWeight = 0;
        for (double weight : weights) {
            totalWeight += weight;
        }

        double randomValue = random.nextDouble() * totalWeight;
        double cumulative = 0;

        for (int i = 0; i < words.size(); i++) {
            cumulative += weights[i];
            if (randomValue <= cumulative) {
                return words.get(i);
            }
        }

        return words.getLast();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Result of Elo rating update.
     */
    public record EloUpdateResult(
        int oldUserRating,
        int newUserRating,
        int userChange,
        int oldWordRating,
        int newWordRating,
        int wordChange,
        double expectedScore
    ) {}
}

package com.vokabelnetz.service;

import com.vokabelnetz.entity.DailyStats;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserWordProgress;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.repository.DailyStatsRepository;
import com.vokabelnetz.repository.UserWordProgressRepository;
import com.vokabelnetz.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Progress and statistics service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProgressService {

    private final UserWordProgressRepository progressRepository;
    private final WordRepository wordRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final StreakService streakService;

    /**
     * Get overall statistics for a user.
     */
    public Map<String, Object> getOverallStats(User user) {
        Long userId = user.getId();

        // Get total counts
        long totalLearned = progressRepository.countByUserIdAndIsLearnedTrue(userId);
        List<UserWordProgress> allProgress = progressRepository.findByUserId(userId);

        long totalReviews = allProgress.stream()
            .mapToLong(p -> p.getTimesCorrect() + p.getTimesIncorrect())
            .sum();
        long correctAnswers = allProgress.stream()
            .mapToLong(UserWordProgress::getTimesCorrect)
            .sum();

        double accuracy = totalReviews > 0 ? (double) correctAnswers / totalReviews * 100 : 0;

        // Overview
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalWordsLearned", totalLearned);
        overview.put("totalWordsInProgress", allProgress.size() - totalLearned);
        overview.put("totalReviews", totalReviews);
        overview.put("correctAnswers", correctAnswers);
        overview.put("incorrectAnswers", totalReviews - correctAnswers);
        overview.put("overallAccuracy", Math.round(accuracy * 10) / 10.0);

        // Elo
        Map<String, Object> elo = new LinkedHashMap<>();
        elo.put("currentRating", user.getEloRating());
        elo.put("highestRating", user.getEloRating()); // TODO: Track highest

        // Streak
        var streakStatus = streakService.getStreakStatus(user);
        Map<String, Object> streak = new LinkedHashMap<>();
        streak.put("currentStreak", streakStatus.currentStreak());
        streak.put("longestStreak", user.getLongestStreak());

        // Level progress
        Map<String, Object> levelProgress = new LinkedHashMap<>();
        for (CefrLevel level : CefrLevel.values()) {
            Map<String, Object> levelInfo = getLevelProgress(user, level);
            levelProgress.put(level.name(), levelInfo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview);
        result.put("elo", elo);
        result.put("streak", streak);
        result.put("levelProgress", levelProgress);

        return result;
    }

    /**
     * Get achievements for a user.
     */
    public Map<String, Object> getAchievements(User user) {
        // Parse achievements from user's JSON field
        // For now, return a basic structure
        List<Map<String, Object>> earned = new ArrayList<>();
        List<Map<String, Object>> available = new ArrayList<>();

        long totalLearned = progressRepository.countByUserIdAndIsLearnedTrue(user.getId());

        // Check earned achievements
        if (totalLearned >= 1) {
            earned.add(createAchievement("FIRST_WORD", "First Word", "Learn your first word", "1"));
        }
        if (totalLearned >= 10) {
            earned.add(createAchievement("WORDS_10", "Beginner", "Learn 10 words", "10"));
        }
        if (totalLearned >= 100) {
            earned.add(createAchievement("WORDS_100", "Century", "Learn 100 words", "100"));
        }
        if (user.getCurrentStreak() >= 7) {
            earned.add(createAchievement("STREAK_7", "Week Warrior", "Maintain a 7-day streak", "7d"));
        }

        // Check available achievements
        if (totalLearned < 10) {
            available.add(createProgressAchievement("WORDS_10", "Beginner", "Learn 10 words", totalLearned, 10));
        }
        if (totalLearned < 100 && totalLearned >= 10) {
            available.add(createProgressAchievement("WORDS_100", "Century", "Learn 100 words", totalLearned, 100));
        }
        if (totalLearned < 250 && totalLearned >= 100) {
            available.add(createProgressAchievement("WORDS_250", "Vocabulary Builder", "Learn 250 words", totalLearned, 250));
        }
        if (user.getCurrentStreak() < 14 && user.getCurrentStreak() >= 7) {
            available.add(createProgressAchievement("STREAK_14", "Fortnight Fighter", "Maintain a 14-day streak", user.getCurrentStreak(), 14));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("earned", earned);
        result.put("available", available);
        result.put("totalEarned", earned.size());
        result.put("totalAvailable", earned.size() + available.size());

        return result;
    }

    /**
     * Get progress for a specific word.
     */
    public UserWordProgress getWordProgress(User user, Long wordId) {
        return progressRepository.findByUserIdAndWordId(user.getId(), wordId)
            .orElse(null);
    }

    /**
     * Get progress for a CEFR level.
     */
    public Map<String, Object> getLevelProgress(User user, CefrLevel level) {
        long total = wordRepository.countByCefrLevel(level);
        List<UserWordProgress> userProgress = progressRepository.findByUserId(user.getId());

        long learned = userProgress.stream()
            .filter(p -> p.getWord().getCefrLevel() == level && Boolean.TRUE.equals(p.getIsLearned()))
            .count();
        long inProgress = userProgress.stream()
            .filter(p -> p.getWord().getCefrLevel() == level && !Boolean.TRUE.equals(p.getIsLearned()))
            .count();

        double percentage = total > 0 ? (double) learned / total * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("learned", learned);
        result.put("inProgress", inProgress);
        result.put("percentage", Math.round(percentage * 10) / 10.0);

        return result;
    }

    /**
     * Get accuracy chart data.
     */
    public Map<String, Object> getAccuracyChart(User user, int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days);

        List<DailyStats> stats = dailyStatsRepository.findByUserIdAndStatDateBetween(
            user.getId(), startDate, today
        );

        List<Map<String, Object>> data = new ArrayList<>();
        for (DailyStats stat : stats) {
            int total = stat.getWordsReviewed() != null ? stat.getWordsReviewed() : 0;
            int correct = stat.getWordsCorrect() != null ? stat.getWordsCorrect() : 0;
            double accuracy = total > 0 ? (double) correct / total * 100 : 0;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", stat.getStatDate().toString());
            point.put("accuracy", Math.round(accuracy * 10) / 10.0);
            point.put("totalReviews", total);
            data.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", days + " days");
        result.put("data", data);

        return result;
    }

    /**
     * Get activity heatmap data.
     */
    public Map<String, Object> getActivityChart(User user, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<DailyStats> stats = dailyStatsRepository.findByUserIdAndStatDateBetween(
            user.getId(), startDate, endDate
        );

        List<Map<String, Object>> activities = new ArrayList<>();
        int maxCount = 0;
        int totalActiveDays = 0;

        for (DailyStats stat : stats) {
            int count = stat.getWordsReviewed() != null ? stat.getWordsReviewed() : 0;
            if (count > 0) {
                totalActiveDays++;
                maxCount = Math.max(maxCount, count);
            }

            int level = getActivityLevel(count);

            Map<String, Object> activity = new LinkedHashMap<>();
            activity.put("date", stat.getStatDate().toString());
            activity.put("count", count);
            activity.put("level", level);
            activities.add(activity);
        }

        Map<String, Object> legend = new LinkedHashMap<>();
        legend.put("0", "No activity");
        legend.put("1", "1-10 words");
        legend.put("2", "11-20 words");
        legend.put("3", "21-30 words");
        legend.put("4", "31+ words");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("activities", activities);
        result.put("totalActiveDays", totalActiveDays);
        result.put("maxDailyCount", maxCount);
        result.put("legend", legend);

        return result;
    }

    private int getActivityLevel(int count) {
        if (count == 0) return 0;
        if (count <= 10) return 1;
        if (count <= 20) return 2;
        if (count <= 30) return 3;
        return 4;
    }

    private Map<String, Object> createAchievement(String type, String name, String description, String icon) {
        Map<String, Object> achievement = new LinkedHashMap<>();
        achievement.put("type", type);
        achievement.put("name", name);
        achievement.put("description", description);
        achievement.put("icon", icon);
        return achievement;
    }

    private Map<String, Object> createProgressAchievement(String type, String name, String description, long current, long target) {
        Map<String, Object> achievement = createAchievement(type, name, description, "");
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("current", current);
        progress.put("target", target);
        progress.put("percentage", Math.round((double) current / target * 100));
        achievement.put("progress", progress);
        return achievement;
    }
}

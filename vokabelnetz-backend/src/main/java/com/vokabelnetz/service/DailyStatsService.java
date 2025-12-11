package com.vokabelnetz.service;

import com.vokabelnetz.entity.DailyStats;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.repository.DailyStatsRepository;
import com.vokabelnetz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Daily statistics service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DailyStatsService {

    private final DailyStatsRepository dailyStatsRepository;
    private final UserRepository userRepository;

    /**
     * Record an answer in daily stats.
     */
    @Transactional
    public void recordAnswer(Long userId, boolean correct, int responseTimeMs) {
        User user = userRepository.getReferenceById(userId);
        ZoneId userZone = ZoneId.of(
            user.getTimezone() != null ? user.getTimezone() : "Europe/Istanbul"
        );
        LocalDate today = LocalDate.now(userZone);

        DailyStats stats = dailyStatsRepository.findByUserIdAndStatDate(userId, today)
            .orElseGet(() -> DailyStats.builder()
                .user(user)
                .statDate(today)
                .build());

        stats.setWordsReviewed(stats.getWordsReviewed() + 1);
        if (correct) {
            stats.setWordsCorrect(stats.getWordsCorrect() + 1);
        }

        dailyStatsRepository.save(stats);
    }

    /**
     * Record a new word learned.
     */
    @Transactional
    public void recordNewWordLearned(Long userId) {
        User user = userRepository.getReferenceById(userId);
        ZoneId userZone = ZoneId.of(
            user.getTimezone() != null ? user.getTimezone() : "Europe/Istanbul"
        );
        LocalDate today = LocalDate.now(userZone);

        DailyStats stats = dailyStatsRepository.findByUserIdAndStatDate(userId, today)
            .orElseGet(() -> DailyStats.builder()
                .user(user)
                .statDate(today)
                .build());

        stats.setNewWordsLearned(stats.getNewWordsLearned() + 1);
        dailyStatsRepository.save(stats);
    }

    /**
     * Record session completion.
     */
    @Transactional
    public void recordSessionCompleted(Long userId, int durationSeconds) {
        User user = userRepository.getReferenceById(userId);
        ZoneId userZone = ZoneId.of(
            user.getTimezone() != null ? user.getTimezone() : "Europe/Istanbul"
        );
        LocalDate today = LocalDate.now(userZone);

        DailyStats stats = dailyStatsRepository.findByUserIdAndStatDate(userId, today)
            .orElseGet(() -> DailyStats.builder()
                .user(user)
                .statDate(today)
                .build());

        stats.setSessionsCompleted(stats.getSessionsCompleted() + 1);
        stats.setTotalTimeSeconds(stats.getTotalTimeSeconds() + durationSeconds);

        dailyStatsRepository.save(stats);
    }

    /**
     * Get stats for date range.
     */
    public List<DailyStats> getStatsForDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return dailyStatsRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * Get today's stats for user.
     */
    public DailyStats getTodayStats(Long userId, String timezone) {
        ZoneId userZone = ZoneId.of(timezone != null ? timezone : "Europe/Istanbul");
        LocalDate today = LocalDate.now(userZone);

        return dailyStatsRepository.findByUserIdAndStatDate(userId, today)
            .orElse(DailyStats.builder()
                .statDate(today)
                .build());
    }

    /**
     * Get total stats for user.
     */
    public TotalStats getTotalStats(Long userId) {
        Object[] result = dailyStatsRepository.getTotalStats(userId);
        long activeDays = dailyStatsRepository.countActiveDays(userId);

        return new TotalStats(
            ((Number) result[0]).longValue(),
            ((Number) result[1]).longValue(),
            ((Number) result[2]).longValue(),
            ((Number) result[3]).longValue(),
            activeDays
        );
    }

    public record TotalStats(
        long totalWordsReviewed,
        long totalWordsCorrect,
        long totalNewWordsLearned,
        long totalTimeSeconds,
        long activeDays
    ) {
        public double accuracy() {
            return totalWordsReviewed > 0
                ? (double) totalWordsCorrect / totalWordsReviewed
                : 0.0;
        }
    }
}

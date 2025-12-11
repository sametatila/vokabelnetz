package com.vokabelnetz.service;

import com.vokabelnetz.config.AppProperties;
import com.vokabelnetz.entity.StreakHistory;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.repository.DailyStatsRepository;
import com.vokabelnetz.repository.StreakHistoryRepository;
import com.vokabelnetz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Streak management service.
 * Based on ALGORITHMS.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StreakService {

    private final UserRepository userRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final StreakHistoryRepository streakHistoryRepository;
    private final AppProperties appProperties;

    /**
     * Get user's timezone with fallback to default.
     */
    private ZoneId getUserTimezone(User user) {
        String timezone = user.getTimezone();
        String defaultTimezone = appProperties.getStreak().getDefaultTimezone();

        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(defaultTimezone);
        }

        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("Invalid timezone '{}' for user {}, using default",
                timezone, user.getId());
            return ZoneId.of(defaultTimezone);
        }
    }

    /**
     * Process end of day for streak calculation.
     */
    @Transactional
    public StreakResult processEndOfDay(User user) {
        ZoneId userZone = getUserTimezone(user);
        LocalDate today = LocalDate.now(userZone);
        LocalDate yesterday = today.minusDays(1);

        // Check if user was active yesterday
        boolean wasActiveYesterday = dailyStatsRepository
            .existsByUserIdAndStatDateAndWordsReviewedGreaterThan(
                user.getId(), yesterday, 0
            );

        if (wasActiveYesterday) {
            return maintainStreak(user, today);
        } else {
            return handleMissedDay(user, today);
        }
    }

    private StreakResult maintainStreak(User user, LocalDate today) {
        var config = appProperties.getStreak();

        int newStreak = user.getCurrentStreak() + 1;
        user.setCurrentStreak(newStreak);

        // Update longest streak
        if (newStreak > user.getLongestStreak()) {
            user.setLongestStreak(newStreak);
        }

        // Award freeze at milestones
        boolean freezeEarned = false;
        if (newStreak % config.getFreezeMilestoneDays() == 0
            && user.getStreakFreezesAvailable() < config.getMaxFreezes()) {
            user.setStreakFreezesAvailable(user.getStreakFreezesAvailable() + 1);
            freezeEarned = true;
            log.info("User {} earned streak freeze at day {}", user.getId(), newStreak);
        }

        // Record history
        saveStreakHistory(user.getId(), today, newStreak, true, false);
        userRepository.save(user);

        return freezeEarned
            ? StreakResult.milestoneReached(newStreak)
            : StreakResult.maintained(newStreak);
    }

    private StreakResult handleMissedDay(User user, LocalDate today) {
        // Try to use freeze
        if (user.getStreakFreezesAvailable() > 0) {
            user.setStreakFreezesAvailable(user.getStreakFreezesAvailable() - 1);
            user.setStreakFreezeUsedAt(today.minusDays(1));

            saveStreakHistory(user.getId(), today.minusDays(1),
                user.getCurrentStreak(), false, true);
            userRepository.save(user);

            log.info("Streak freeze used for user {}", user.getId());
            return StreakResult.frozen(user.getCurrentStreak());
        }

        // Streak broken
        int lostStreak = user.getCurrentStreak();
        user.setCurrentStreak(0);

        saveStreakHistory(user.getId(), today.minusDays(1), 0, false, false);
        userRepository.save(user);

        log.info("Streak broken for user {}, lost {} days", user.getId(), lostStreak);
        return StreakResult.broken(lostStreak);
    }

    /**
     * Get current streak status for display.
     */
    public StreakStatus getStreakStatus(User user) {
        ZoneId userZone = getUserTimezone(user);
        LocalDate today = LocalDate.now(userZone);

        boolean completedToday = dailyStatsRepository
            .existsByUserIdAndStatDateAndWordsReviewedGreaterThan(
                user.getId(), today, 0
            );

        LocalTime now = LocalTime.now(userZone);
        LocalTime endOfDay = LocalTime.of(23, 59);
        long minutesRemaining = now.until(endOfDay, ChronoUnit.MINUTES);

        return new StreakStatus(
            user.getCurrentStreak(),
            user.getLongestStreak(),
            completedToday,
            !completedToday && minutesRemaining < 120, // at risk if <2 hours left
            user.getStreakFreezesAvailable(),
            minutesRemaining
        );
    }

    /**
     * Manually activate freeze for today.
     */
    @Transactional
    public boolean activateFreeze(User user) {
        if (user.getStreakFreezesAvailable() <= 0) {
            return false;
        }

        ZoneId userZone = getUserTimezone(user);
        LocalDate today = LocalDate.now(userZone);

        user.setStreakFreezesAvailable(user.getStreakFreezesAvailable() - 1);
        user.setStreakFreezeUsedAt(today);

        saveStreakHistory(user.getId(), today, user.getCurrentStreak(), false, true);
        userRepository.save(user);

        log.info("User {} manually activated streak freeze", user.getId());
        return true;
    }

    private void saveStreakHistory(Long userId, LocalDate date, int streakCount,
                                   boolean wasActive, boolean freezeUsed) {
        StreakHistory history = StreakHistory.builder()
            .user(userRepository.getReferenceById(userId))
            .streakDate(date)
            .streakCount(streakCount)
            .wasActive(wasActive)
            .freezeUsed(freezeUsed)
            .build();

        streakHistoryRepository.save(history);
    }

    // DTOs
    public record StreakResult(
        StreakResultType type,
        int currentStreak,
        int lostStreak,
        boolean freezeEarned
    ) {
        public static StreakResult maintained(int streak) {
            return new StreakResult(StreakResultType.MAINTAINED, streak, 0, false);
        }

        public static StreakResult broken(int lost) {
            return new StreakResult(StreakResultType.BROKEN, 0, lost, false);
        }

        public static StreakResult frozen(int streak) {
            return new StreakResult(StreakResultType.FROZEN, streak, 0, false);
        }

        public static StreakResult milestoneReached(int streak) {
            return new StreakResult(StreakResultType.MILESTONE, streak, 0, true);
        }
    }

    public enum StreakResultType {
        MAINTAINED, BROKEN, FROZEN, MILESTONE
    }

    public record StreakStatus(
        int currentStreak,
        int longestStreak,
        boolean completedToday,
        boolean atRisk,
        int freezesAvailable,
        long minutesUntilReset
    ) {}
}

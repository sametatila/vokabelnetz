package com.vokabelnetz.scheduler;

import com.vokabelnetz.entity.DailyStats;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserPreferences;
import com.vokabelnetz.repository.DailyStatsRepository;
import com.vokabelnetz.repository.UserPreferencesRepository;
import com.vokabelnetz.repository.UserRepository;
import com.vokabelnetz.service.EmailService;
import com.vokabelnetz.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduled tasks for streak management.
 * Based on ALGORITHMS.md documentation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreakScheduler {

    private final UserRepository userRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final StreakService streakService;
    private final EmailService emailService;

    /**
     * Check and update streaks at midnight (user's timezone).
     * Runs every hour to catch different timezones.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at :00
    @Transactional
    public void processStreaks() {
        log.info("Starting hourly streak processing...");

        List<User> activeUsers = userRepository.findByIsActiveTrueAndDeletedAtIsNull();
        int processed = 0;
        int broken = 0;
        int freezeUsed = 0;

        for (User user : activeUsers) {
            try {
                String timezone = user.getTimezone() != null ? user.getTimezone() : "Europe/Istanbul";
                ZoneId zoneId = ZoneId.of(timezone);
                LocalTime nowInUserTz = LocalTime.now(zoneId);

                // Only process if it's midnight in user's timezone (between 00:00 and 00:59)
                if (nowInUserTz.getHour() != 0) {
                    continue;
                }

                LocalDate yesterday = LocalDate.now(zoneId).minusDays(1);
                boolean wasActiveYesterday = wasUserActiveOnDate(user.getId(), yesterday);

                if (!wasActiveYesterday) {
                    if (user.getCurrentStreak() > 0) {
                        // Try to use streak freeze
                        if (user.getStreakFreezesAvailable() > 0) {
                            user.setStreakFreezesAvailable(user.getStreakFreezesAvailable() - 1);
                            user.setStreakFreezeUsedAt(yesterday);
                            userRepository.save(user);
                            freezeUsed++;
                            log.debug("Streak freeze used for user {}", user.getId());
                        } else {
                            // Break streak
                            int oldStreak = user.getCurrentStreak();
                            user.setCurrentStreak(0);
                            userRepository.save(user);
                            broken++;
                            log.debug("Streak broken for user {} (was {} days)", user.getId(), oldStreak);
                        }
                    }
                }

                processed++;
            } catch (Exception e) {
                log.error("Error processing streak for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Streak processing completed: {} users processed, {} streaks broken, {} freezes used",
            processed, broken, freezeUsed);
    }

    /**
     * Send streak reminder emails.
     * Runs at 20:00 every day (server time) - users who haven't practiced today.
     */
    @Scheduled(cron = "0 0 20 * * *") // 20:00 every day
    @Transactional(readOnly = true)
    public void sendStreakReminders() {
        log.info("Starting streak reminder job...");

        List<User> activeUsers = userRepository.findByIsActiveTrueAndDeletedAtIsNull();
        int sent = 0;

        for (User user : activeUsers) {
            try {
                // Check if user has streak reminders enabled
                UserPreferences prefs = preferencesRepository.findByUserId(user.getId()).orElse(null);
                if (prefs == null || !Boolean.TRUE.equals(prefs.getStreakReminders())) {
                    continue;
                }

                // Only remind users with active streaks
                if (user.getCurrentStreak() == 0) {
                    continue;
                }

                String timezone = user.getTimezone() != null ? user.getTimezone() : "Europe/Istanbul";
                ZoneId zoneId = ZoneId.of(timezone);
                LocalDate today = LocalDate.now(zoneId);

                // Check if user was active today
                if (!wasUserActiveOnDate(user.getId(), today)) {
                    emailService.sendStreakReminderEmail(user, user.getCurrentStreak());
                    sent++;
                }
            } catch (Exception e) {
                log.error("Error sending streak reminder for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Streak reminders sent: {}", sent);
    }

    /**
     * Award streak freezes for milestones.
     * Runs daily at 01:00.
     */
    @Scheduled(cron = "0 0 1 * * *") // 01:00 every day
    @Transactional
    public void awardStreakFreezes() {
        log.info("Starting streak freeze award job...");

        List<User> activeUsers = userRepository.findByIsActiveTrueAndDeletedAtIsNull();
        int awarded = 0;

        for (User user : activeUsers) {
            try {
                // Award freeze every 7 days of streak (max 3)
                int streak = user.getCurrentStreak();
                int currentFreezes = user.getStreakFreezesAvailable();
                int maxFreezes = 3;

                if (currentFreezes < maxFreezes && streak > 0 && streak % 7 == 0) {
                    // Check if we already awarded for this milestone today
                    String timezone = user.getTimezone() != null ? user.getTimezone() : "Europe/Istanbul";
                    ZoneId zoneId = ZoneId.of(timezone);
                    LocalDate today = LocalDate.now(zoneId);

                    // Simple check: was streak updated today?
                    if (wasUserActiveOnDate(user.getId(), today.minusDays(1))) {
                        user.setStreakFreezesAvailable(currentFreezes + 1);
                        userRepository.save(user);
                        awarded++;
                        log.debug("Streak freeze awarded to user {} for {} day streak",
                            user.getId(), streak);
                    }
                }
            } catch (Exception e) {
                log.error("Error awarding streak freeze for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Streak freezes awarded: {}", awarded);
    }

    /**
     * Check if user was active (had any activity) on a specific date.
     */
    private boolean wasUserActiveOnDate(Long userId, LocalDate date) {
        return dailyStatsRepository.findByUserIdAndStatDate(userId, date)
            .map(stats -> stats.getWordsReviewed() > 0 || stats.getNewWordsLearned() > 0)
            .orElse(false);
    }
}

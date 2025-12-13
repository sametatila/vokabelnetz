package com.vokabelnetz.scheduler;

import com.vokabelnetz.entity.DailyStats;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserPreferences;
import com.vokabelnetz.repository.DailyStatsRepository;
import com.vokabelnetz.repository.UserPreferencesRepository;
import com.vokabelnetz.repository.UserRepository;
import com.vokabelnetz.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduled task for sending weekly progress reports.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportScheduler {

    private final UserRepository userRepository;
    private final DailyStatsRepository dailyStatsRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final EmailService emailService;

    /**
     * Send weekly progress reports.
     * Runs every Monday at 09:00.
     */
    @Scheduled(cron = "0 0 9 * * MON") // 09:00 every Monday
    @Transactional(readOnly = true)
    public void sendWeeklyReports() {
        log.info("Starting weekly report job...");

        List<User> activeUsers = userRepository.findByIsActiveTrueAndDeletedAtIsNull();
        int sent = 0;

        for (User user : activeUsers) {
            try {
                // Check if user has weekly reports enabled
                UserPreferences prefs = preferencesRepository.findByUserId(user.getId()).orElse(null);
                if (prefs == null || !Boolean.TRUE.equals(prefs.getWeeklyReport())) {
                    continue;
                }

                // Calculate weekly stats
                String timezone = user.getTimezone() != null ? user.getTimezone() : "Europe/Istanbul";
                ZoneId zoneId = ZoneId.of(timezone);
                LocalDate today = LocalDate.now(zoneId);
                LocalDate weekAgo = today.minusDays(7);

                List<DailyStats> weeklyStats = dailyStatsRepository.findByUserIdAndDateRange(
                    user.getId(), weekAgo, today
                );

                int totalWordsLearned = weeklyStats.stream()
                    .mapToInt(s -> s.getNewWordsLearned() != null ? s.getNewWordsLearned() : 0)
                    .sum();

                int totalWordsReviewed = weeklyStats.stream()
                    .mapToInt(s -> s.getWordsReviewed() != null ? s.getWordsReviewed() : 0)
                    .sum();

                // Send email with stats
                emailService.sendWeeklyReportEmail(
                    user,
                    totalWordsLearned,
                    totalWordsReviewed,
                    user.getCurrentStreak()
                );

                sent++;
            } catch (Exception e) {
                log.error("Error sending weekly report for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Weekly reports sent: {}", sent);
    }
}

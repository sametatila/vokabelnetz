package com.vokabelnetz.controller;

import com.vokabelnetz.dto.response.ApiResponse;
import com.vokabelnetz.entity.DailyStats;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserWordProgress;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.security.CurrentUser;
import com.vokabelnetz.service.DailyStatsService;
import com.vokabelnetz.service.ProgressService;
import com.vokabelnetz.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Progress and statistics controller.
 * Based on API.md documentation.
 */
@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final DailyStatsService dailyStatsService;
    private final StreakService streakService;
    private final ProgressService progressService;

    /**
     * Get overall statistics.
     * GET /api/progress/overall
     */
    @GetMapping("/overall")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverallStats(
        @CurrentUser User user
    ) {
        Map<String, Object> stats = progressService.getOverallStats(user);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get today's statistics.
     * GET /api/progress/daily
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyStats>> getDailyStats(@CurrentUser User user) {
        DailyStats stats = dailyStatsService.getTodayStats(user.getId(), user.getTimezone());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get weekly statistics.
     * GET /api/progress/weekly
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<DailyStats>>> getWeeklyStats(@CurrentUser User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        List<DailyStats> stats = dailyStatsService.getStatsForDateRange(user.getId(), weekAgo, today);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get monthly statistics.
     * GET /api/progress/monthly
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<DailyStats>>> getMonthlyStats(@CurrentUser User user) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);
        List<DailyStats> stats = dailyStatsService.getStatsForDateRange(user.getId(), monthAgo, today);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get streak status.
     * GET /api/progress/streak
     */
    @GetMapping("/streak")
    public ResponseEntity<ApiResponse<StreakService.StreakStatus>> getStreakStatus(
        @CurrentUser User user
    ) {
        StreakService.StreakStatus status = streakService.getStreakStatus(user);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Get achievements.
     * GET /api/progress/achievements
     */
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAchievements(
        @CurrentUser User user
    ) {
        Map<String, Object> achievements = progressService.getAchievements(user);
        return ResponseEntity.ok(ApiResponse.success(achievements));
    }

    /**
     * Get word-specific progress.
     * GET /api/progress/words/{wordId}
     */
    @GetMapping("/words/{wordId}")
    public ResponseEntity<ApiResponse<UserWordProgress>> getWordProgress(
        @CurrentUser User user,
        @PathVariable Long wordId
    ) {
        UserWordProgress progress = progressService.getWordProgress(user, wordId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * Get level progress.
     * GET /api/progress/level/{cefr}
     */
    @GetMapping("/level/{cefr}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLevelProgress(
        @CurrentUser User user,
        @PathVariable CefrLevel cefr
    ) {
        Map<String, Object> progress = progressService.getLevelProgress(user, cefr);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * Get accuracy chart data.
     * GET /api/progress/charts/accuracy
     */
    @GetMapping("/charts/accuracy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccuracyChart(
        @CurrentUser User user,
        @RequestParam(defaultValue = "30") int days
    ) {
        Map<String, Object> chartData = progressService.getAccuracyChart(user, days);
        return ResponseEntity.ok(ApiResponse.success(chartData));
    }

    /**
     * Get activity heatmap data.
     * GET /api/progress/charts/activity
     */
    @GetMapping("/charts/activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActivityChart(
        @CurrentUser User user,
        @RequestParam(defaultValue = "2025") int year
    ) {
        Map<String, Object> chartData = progressService.getActivityChart(user, year);
        return ResponseEntity.ok(ApiResponse.success(chartData));
    }

    /**
     * Activate streak freeze.
     * POST /api/progress/streak/freeze
     */
    @PostMapping("/streak/freeze")
    public ResponseEntity<ApiResponse<?>> activateFreeze(
        @CurrentUser User user
    ) {
        boolean success = streakService.activateFreeze(user);

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "message", "Streak freeze activated",
                "freezesRemaining", user.getStreakFreezesAvailable()
            )));
        } else {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("NO_FREEZES", "No streak freezes available")
            );
        }
    }

    // Legacy endpoints renamed

    /**
     * Get today's statistics (legacy - maps to /daily).
     * GET /api/progress/today
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<DailyStats>> getTodayStats(@CurrentUser User user) {
        return getDailyStats(user);
    }

    /**
     * Get statistics for date range.
     * GET /api/progress/range?start=2024-01-01&end=2024-01-31
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<DailyStats>>> getStatsRange(
        @CurrentUser User user,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<DailyStats> stats = dailyStatsService.getStatsForDateRange(user.getId(), start, end);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get total statistics.
     * GET /api/progress/total
     */
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<DailyStatsService.TotalStats>> getTotalStats(
        @CurrentUser User user
    ) {
        DailyStatsService.TotalStats stats = dailyStatsService.getTotalStats(user.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

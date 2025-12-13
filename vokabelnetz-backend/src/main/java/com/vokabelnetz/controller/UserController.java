package com.vokabelnetz.controller;

import com.vokabelnetz.dto.request.ChangePasswordRequest;
import com.vokabelnetz.dto.request.UpdateLanguageRequest;
import com.vokabelnetz.dto.request.UpdatePreferencesRequest;
import com.vokabelnetz.dto.request.UpdateProfileRequest;
import com.vokabelnetz.dto.response.*;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserPreferences;
import com.vokabelnetz.entity.enums.SourceLanguage;
import com.vokabelnetz.entity.enums.UiLanguage;
import com.vokabelnetz.exception.BadRequestException;
import com.vokabelnetz.security.CurrentUser;
import com.vokabelnetz.service.AuthService;
import com.vokabelnetz.service.EmailService;
import com.vokabelnetz.service.PasswordHistoryService;
import com.vokabelnetz.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * User profile and settings controller.
 * Based on API.md documentation.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;
    private final EmailService emailService;

    /**
     * Get current user profile.
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
        @CurrentUser User user
    ) {
        UserProfileResponse response = mapToProfileResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update current user profile.
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
        @CurrentUser User user,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        User updated = userService.save(user);
        log.info("User {} updated profile", user.getId());

        return ResponseEntity.ok(ApiResponse.success(mapToProfileResponse(updated)));
    }

    /**
     * Change password.
     * PUT /api/users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
        @CurrentUser User user,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Check password history (prevent reuse of last N passwords)
        if (passwordHistoryService.isPasswordUsedBefore(user, request.getNewPassword())) {
            throw new BadRequestException("Cannot reuse a recent password. Please choose a different password.");
        }

        // Save current password to history before changing
        passwordHistoryService.savePasswordHash(user, user.getPasswordHash());

        // Update password
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        user.setPasswordChangedAt(LocalDateTime.now());
        userService.save(user);

        // Revoke all refresh tokens (security best practice)
        authService.logoutAll(user.getId());

        // Send notification email
        emailService.sendPasswordChangedNotification(user);

        log.info("User {} changed password", user.getId());

        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Password changed successfully. Please login again.")
        ));
    }

    /**
     * Delete account (soft delete - GDPR).
     * DELETE /api/users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAccount(
        @CurrentUser User user,
        @RequestBody(required = false) Map<String, String> request
    ) {
        String reason = request != null ? request.get("reason") : null;

        // Soft delete
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletionReason(reason);
        user.setIsActive(false);
        userService.save(user);

        // Revoke all sessions
        authService.logoutAll(user.getId());

        // Send deletion confirmation email
        emailService.sendAccountDeletedNotification(user);

        log.info("User {} deleted account", user.getId());

        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "Account deleted successfully")
        ));
    }

    /**
     * Get user preferences.
     * GET /api/users/me/preferences
     */
    @GetMapping("/me/preferences")
    public ResponseEntity<ApiResponse<PreferencesResponse>> getPreferences(
        @CurrentUser User user
    ) {
        UserPreferences prefs = userService.getOrCreatePreferences(user);
        return ResponseEntity.ok(ApiResponse.success(mapToPreferencesResponse(prefs)));
    }

    /**
     * Update user preferences.
     * PUT /api/users/me/preferences
     */
    @PutMapping("/me/preferences")
    public ResponseEntity<ApiResponse<PreferencesResponse>> updatePreferences(
        @CurrentUser User user,
        @RequestBody UpdatePreferencesRequest request
    ) {
        UserPreferences prefs = userService.getOrCreatePreferences(user);

        // Update fields if provided
        if (request.getEmailNotifications() != null) {
            prefs.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getStreakReminders() != null) {
            prefs.setStreakReminders(request.getStreakReminders());
        }
        if (request.getWeeklyReport() != null) {
            prefs.setWeeklyReport(request.getWeeklyReport());
        }
        if (request.getAutoPlayAudio() != null) {
            prefs.setAutoPlayAudio(request.getAutoPlayAudio());
        }
        if (request.getShowExampleSentences() != null) {
            prefs.setShowExampleSentences(request.getShowExampleSentences());
        }
        if (request.getShowPronunciation() != null) {
            prefs.setShowPronunciation(request.getShowPronunciation());
        }
        if (request.getWordsPerSession() != null) {
            prefs.setWordsPerSession(request.getWordsPerSession());
        }
        if (request.getReviewPriority() != null) {
            prefs.setReviewPriority(request.getReviewPriority());
        }
        if (request.getDarkMode() != null) {
            prefs.setDarkMode(request.getDarkMode());
        }
        if (request.getCompactMode() != null) {
            prefs.setCompactMode(request.getCompactMode());
        }
        if (request.getKeyboardShortcutsEnabled() != null) {
            prefs.setKeyboardShortcutsEnabled(request.getKeyboardShortcutsEnabled());
        }
        if (request.getReminderTime() != null) {
            prefs.setReminderTime(request.getReminderTime());
        }

        UserPreferences updated = userService.updatePreferences(user.getId(), prefs);
        log.info("User {} updated preferences", user.getId());

        return ResponseEntity.ok(ApiResponse.success(mapToPreferencesResponse(updated)));
    }

    /**
     * Get language settings.
     * GET /api/users/me/language
     */
    @GetMapping("/me/language")
    public ResponseEntity<ApiResponse<LanguageSettingsResponse>> getLanguageSettings(
        @CurrentUser User user
    ) {
        LanguageSettingsResponse response = LanguageSettingsResponse.builder()
            .uiLanguage(user.getUiLanguage() != null ? user.getUiLanguage().name() : "EN")
            .sourceLanguage(user.getSourceLanguage() != null ? user.getSourceLanguage().name() : "EN")
            .targetLanguage("DE") // German is always the target
            .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update language settings.
     * PUT /api/users/me/language
     */
    @PutMapping("/me/language")
    public ResponseEntity<ApiResponse<LanguageSettingsResponse>> updateLanguageSettings(
        @CurrentUser User user,
        @RequestBody UpdateLanguageRequest request
    ) {
        if (request.getUiLanguage() != null) {
            try {
                user.setUiLanguage(UiLanguage.valueOf(request.getUiLanguage().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid UI language: " + request.getUiLanguage());
            }
        }
        if (request.getSourceLanguage() != null) {
            try {
                user.setSourceLanguage(SourceLanguage.valueOf(request.getSourceLanguage().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid source language: " + request.getSourceLanguage());
            }
        }

        User updated = userService.save(user);
        log.info("User {} updated language settings", user.getId());

        LanguageSettingsResponse response = LanguageSettingsResponse.builder()
            .uiLanguage(updated.getUiLanguage().name())
            .sourceLanguage(updated.getSourceLanguage().name())
            .targetLanguage("DE")
            .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Quick switch source language.
     * PATCH /api/users/me/language/source
     */
    @PatchMapping("/me/language/source")
    public ResponseEntity<ApiResponse<LanguageSettingsResponse>> switchSourceLanguage(
        @CurrentUser User user,
        @RequestBody Map<String, String> request
    ) {
        String sourceLanguage = request.get("sourceLanguage");
        if (sourceLanguage == null) {
            throw new BadRequestException("sourceLanguage is required");
        }

        try {
            user.setSourceLanguage(SourceLanguage.valueOf(sourceLanguage.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid source language: " + sourceLanguage);
        }

        User updated = userService.save(user);
        log.info("User {} switched source language to {}", user.getId(), sourceLanguage);

        LanguageSettingsResponse response = LanguageSettingsResponse.builder()
            .uiLanguage(updated.getUiLanguage().name())
            .sourceLanguage(updated.getSourceLanguage().name())
            .targetLanguage("DE")
            .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .avatarUrl(user.getAvatarUrl())
            .eloRating(user.getEloRating())
            .currentStreak(user.getCurrentStreak())
            .longestStreak(user.getLongestStreak())
            .totalWordsLearned(user.getTotalWordsLearned())
            .dailyGoal(user.getDailyGoal())
            .uiLanguage(user.getUiLanguage() != null ? user.getUiLanguage().name() : "EN")
            .sourceLanguage(user.getSourceLanguage() != null ? user.getSourceLanguage().name() : "EN")
            .timezone(user.getTimezone())
            .isActive(user.getIsActive())
            .emailVerified(user.getEmailVerified())
            .lastActiveAt(user.getLastActiveAt())
            .createdAt(user.getCreatedAt())
            .build();
    }

    private PreferencesResponse mapToPreferencesResponse(UserPreferences prefs) {
        return PreferencesResponse.builder()
            .emailNotifications(prefs.getEmailNotifications())
            .streakReminders(prefs.getStreakReminders())
            .weeklyReport(prefs.getWeeklyReport())
            .autoPlayAudio(prefs.getAutoPlayAudio())
            .showExampleSentences(prefs.getShowExampleSentences())
            .showPronunciation(prefs.getShowPronunciation())
            .wordsPerSession(prefs.getWordsPerSession())
            .reviewPriority(prefs.getReviewPriority())
            .darkMode(prefs.getDarkMode())
            .compactMode(prefs.getCompactMode())
            .keyboardShortcutsEnabled(prefs.getKeyboardShortcutsEnabled())
            .reminderTime(prefs.getReminderTime())
            .build();
    }
}

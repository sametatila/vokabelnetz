package com.vokabelnetz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * User preferences for customizable settings.
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Notification settings
    @Builder.Default
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Builder.Default
    @Column(name = "streak_reminders")
    private Boolean streakReminders = true;

    @Builder.Default
    @Column(name = "weekly_report")
    private Boolean weeklyReport = true;

    // Learning preferences
    @Builder.Default
    @Column(name = "auto_play_audio")
    private Boolean autoPlayAudio = true;

    @Builder.Default
    @Column(name = "show_example_sentences")
    private Boolean showExampleSentences = true;

    @Builder.Default
    @Column(name = "show_pronunciation")
    private Boolean showPronunciation = true;

    // Session settings
    @Builder.Default
    @Column(name = "words_per_session")
    private Integer wordsPerSession = 20;

    @Builder.Default
    @Column(name = "review_priority")
    private String reviewPriority = "DUE_FIRST";

    // UI preferences
    @Builder.Default
    @Column(name = "dark_mode")
    private Boolean darkMode = false;

    @Builder.Default
    @Column(name = "compact_mode")
    private Boolean compactMode = false;

    // Keyboard shortcuts
    @Builder.Default
    @Column(name = "keyboard_shortcuts_enabled")
    private Boolean keyboardShortcutsEnabled = true;

    // Custom keyboard bindings (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keyboard_bindings", columnDefinition = "jsonb")
    private String keyboardBindings;

    // Reminder time (format: HH:mm)
    @Column(name = "reminder_time", length = 5)
    private String reminderTime;
}

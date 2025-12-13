package com.vokabelnetz.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vokabelnetz.entity.enums.Role;
import com.vokabelnetz.entity.enums.SourceLanguage;
import com.vokabelnetz.entity.enums.UiLanguage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User entity representing application users.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // Elo Rating
    @Builder.Default
    @Column(name = "elo_rating")
    private Integer eloRating = 1000;

    // Streaks
    @Builder.Default
    @Column(name = "current_streak")
    private Integer currentStreak = 0;

    @Builder.Default
    @Column(name = "longest_streak")
    private Integer longestStreak = 0;

    @Builder.Default
    @Column(name = "streak_freezes_available")
    private Integer streakFreezesAvailable = 0;

    @Column(name = "streak_freeze_used_at")
    private LocalDate streakFreezeUsedAt;

    // Stats
    @Builder.Default
    @Column(name = "total_words_learned")
    private Integer totalWordsLearned = 0;

    @Builder.Default
    @Column(name = "daily_goal")
    private Integer dailyGoal = 20;

    // Language Settings
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ui_language")
    private UiLanguage uiLanguage = UiLanguage.EN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "source_language")
    private SourceLanguage sourceLanguage = SourceLanguage.EN;

    // Timezone
    @Builder.Default
    @Column(length = 50)
    private String timezone = "Europe/Istanbul";

    // Status
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    // Soft Delete (GDPR)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deletion_reason")
    private String deletionReason;

    // Achievements (JSONB)
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String achievements = "[]";

    // Role
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role = Role.ROLE_USER;

    // Password tracking
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // Activity tracking
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    // Relationships - excluded from JSON to prevent lazy loading issues
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserWordProgress> wordProgress = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LearningSession> learningSessions = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences preferences;
}

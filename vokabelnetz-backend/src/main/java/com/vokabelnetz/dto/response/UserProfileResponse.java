package com.vokabelnetz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Integer eloRating;
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer totalWordsLearned;
    private Integer dailyGoal;
    private String uiLanguage;
    private String sourceLanguage;
    private String timezone;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
}

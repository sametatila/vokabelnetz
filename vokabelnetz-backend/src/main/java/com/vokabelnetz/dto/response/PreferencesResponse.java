package com.vokabelnetz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesResponse {
    private Boolean emailNotifications;
    private Boolean streakReminders;
    private Boolean weeklyReport;
    private Boolean autoPlayAudio;
    private Boolean showExampleSentences;
    private Boolean showPronunciation;
    private Integer wordsPerSession;
    private String reviewPriority;
    private Boolean darkMode;
    private Boolean compactMode;
    private Boolean keyboardShortcutsEnabled;
    private String reminderTime;
}

package com.vokabelnetz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application-specific configuration properties.
 * All configurable values from application.yml are centralized here.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Data data = new Data();
    private final Algorithm algorithm = new Algorithm();
    private final Streak streak = new Streak();
    private final Security security = new Security();

    @Getter
    @Setter
    public static class Data {
        private String seedMode = "VALIDATE";
        private String path = "classpath:data/";
    }

    @Getter
    @Setter
    public static class Algorithm {
        // SM-2 Algorithm defaults
        private double minEaseFactor = 1.3;
        private double maxEaseFactor = 5.0;
        private double defaultEaseFactor = 2.5;
        private int maxInterval = 365;
        private int learnedThresholdDays = 21;

        // Elo Rating defaults
        private int kFactor = 32;
        private int minRating = 100;
        private int maxRating = 3000;
        private int defaultRating = 1000;
        private int matchTolerance = 200;
    }

    @Getter
    @Setter
    public static class Streak {
        private int freezeMilestoneDays = 7;
        private int maxFreezes = 3;
        private String defaultTimezone = "Europe/Istanbul";
    }

    @Getter
    @Setter
    public static class Security {
        private int maxActiveSessions = 5;
        private int maxLoginAttempts = 5;
        private int lockoutMinutes = 15;
        private int passwordResetExpirationHours = 1;
        private int maxPasswordResetRequestsPerHour = 3;
    }
}

package com.vokabelnetz.service;

import com.vokabelnetz.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Security alert service for sending notifications about security events.
 * Based on SECURITY.md documentation.
 *
 * Supports multiple alert channels:
 * - Logging (always enabled)
 * - Email (optional)
 * - Slack webhook (optional)
 * - Sentry (optional, via logging integration)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAlertService {

    private final EmailService emailService;

    @Value("${app.security.alerts.email:#{null}}")
    private String alertEmail;

    @Value("${app.security.alerts.slack-webhook:#{null}}")
    private String slackWebhook;

    @Value("${app.security.alerts.enabled:true}")
    private boolean alertsEnabled;

    /**
     * Alert types for categorization and filtering.
     */
    public enum AlertType {
        BRUTE_FORCE,
        TOKEN_REUSE,
        DISTRIBUTED_ATTACK,
        ACCOUNT_LOCKOUT,
        PASSWORD_RESET_ABUSE,
        SUSPICIOUS_ACTIVITY,
        SECURITY_ERROR
    }

    /**
     * Alert severity levels.
     */
    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Send alert for refresh token reuse detection (potential theft).
     * Severity: CRITICAL
     */
    @Async
    public void sendTokenReuseAlert(User user, String ipAddress, String userAgent) {
        SecurityAlert alert = SecurityAlert.builder()
            .type(AlertType.TOKEN_REUSE)
            .severity(AlertSeverity.CRITICAL)
            .title("Refresh Token Reuse Detected")
            .message("A revoked refresh token was reused, indicating potential token theft. " +
                     "All sessions for this user have been terminated.")
            .userId(user.getId())
            .userEmail(maskEmail(user.getEmail()))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .timestamp(Instant.now())
            .build();

        sendAlert(alert);
    }

    /**
     * Send alert for distributed brute force attack detection.
     * Severity: HIGH
     */
    @Async
    public void sendDistributedAttackAlert(String email, int uniqueIpCount) {
        SecurityAlert alert = SecurityAlert.builder()
            .type(AlertType.DISTRIBUTED_ATTACK)
            .severity(AlertSeverity.HIGH)
            .title("Distributed Brute Force Attack Detected")
            .message(String.format(
                "Multiple IPs (%d unique) are attempting to login to the same account, " +
                "indicating a coordinated attack.", uniqueIpCount))
            .userEmail(maskEmail(email))
            .metadata(Map.of("uniqueIpCount", String.valueOf(uniqueIpCount)))
            .timestamp(Instant.now())
            .build();

        sendAlert(alert);
    }

    /**
     * Send alert for account lockout due to failed attempts.
     * Severity: MEDIUM
     */
    @Async
    public void sendAccountLockoutAlert(String email, String ipAddress, int failedAttempts) {
        SecurityAlert alert = SecurityAlert.builder()
            .type(AlertType.ACCOUNT_LOCKOUT)
            .severity(AlertSeverity.MEDIUM)
            .title("Account Locked Due to Failed Attempts")
            .message(String.format(
                "Account locked after %d failed login attempts from IP: %s",
                failedAttempts, ipAddress))
            .userEmail(maskEmail(email))
            .ipAddress(ipAddress)
            .metadata(Map.of("failedAttempts", String.valueOf(failedAttempts)))
            .timestamp(Instant.now())
            .build();

        sendAlert(alert);
    }

    /**
     * Send alert for brute force attempt on single account.
     * Severity: MEDIUM
     */
    @Async
    public void sendBruteForceAlert(String email, String ipAddress, int attempts) {
        SecurityAlert alert = SecurityAlert.builder()
            .type(AlertType.BRUTE_FORCE)
            .severity(AlertSeverity.MEDIUM)
            .title("Brute Force Attack Detected")
            .message(String.format(
                "Multiple failed login attempts (%d) detected for account from IP: %s",
                attempts, ipAddress))
            .userEmail(maskEmail(email))
            .ipAddress(ipAddress)
            .metadata(Map.of("attempts", String.valueOf(attempts)))
            .timestamp(Instant.now())
            .build();

        sendAlert(alert);
    }

    /**
     * Send alert for password reset abuse (rate limit hit).
     * Severity: LOW
     */
    @Async
    public void sendPasswordResetAbuseAlert(String email, String ipAddress) {
        SecurityAlert alert = SecurityAlert.builder()
            .type(AlertType.PASSWORD_RESET_ABUSE)
            .severity(AlertSeverity.LOW)
            .title("Password Reset Rate Limit Exceeded")
            .message("Too many password reset requests for account.")
            .userEmail(maskEmail(email))
            .ipAddress(ipAddress)
            .timestamp(Instant.now())
            .build();

        sendAlert(alert);
    }

    /**
     * Send alert for suspicious activity.
     * Severity: Variable
     */
    @Async
    public void sendSuspiciousActivityAlert(String description, User user, String ipAddress,
                                            AlertSeverity severity, Map<String, String> metadata) {
        SecurityAlert alert = SecurityAlert.builder()
            .type(AlertType.SUSPICIOUS_ACTIVITY)
            .severity(severity)
            .title("Suspicious Activity Detected")
            .message(description)
            .userId(user != null ? user.getId() : null)
            .userEmail(user != null ? maskEmail(user.getEmail()) : null)
            .ipAddress(ipAddress)
            .metadata(metadata)
            .timestamp(Instant.now())
            .build();

        sendAlert(alert);
    }

    /**
     * Send alert for security-related errors.
     * Severity: HIGH
     */
    @Async
    public void sendSecurityErrorAlert(String description, Exception error, Map<String, String> context) {
        SecurityAlert alert = SecurityAlert.builder()
            .type(AlertType.SECURITY_ERROR)
            .severity(AlertSeverity.HIGH)
            .title("Security System Error")
            .message(description + (error != null ? ": " + error.getMessage() : ""))
            .metadata(context)
            .timestamp(Instant.now())
            .build();

        sendAlert(alert);
    }

    /**
     * Core alert sending method - dispatches to all configured channels.
     */
    private void sendAlert(SecurityAlert alert) {
        if (!alertsEnabled) {
            log.debug("Security alerts disabled, skipping alert: {}", alert.getTitle());
            return;
        }

        // Always log the alert
        logAlert(alert);

        // Send to Slack if configured
        if (slackWebhook != null && !slackWebhook.isBlank()) {
            sendToSlack(alert);
        }

        // Send email if configured and severity is HIGH or CRITICAL
        if (alertEmail != null && !alertEmail.isBlank() &&
            (alert.getSeverity() == AlertSeverity.HIGH || alert.getSeverity() == AlertSeverity.CRITICAL)) {
            sendToEmail(alert);
        }
    }

    /**
     * Log alert with appropriate level based on severity.
     */
    private void logAlert(SecurityAlert alert) {
        String logMessage = formatAlertForLog(alert);

        switch (alert.getSeverity()) {
            case CRITICAL -> log.error("SECURITY_ALERT [CRITICAL]: {}", logMessage);
            case HIGH -> log.error("SECURITY_ALERT [HIGH]: {}", logMessage);
            case MEDIUM -> log.warn("SECURITY_ALERT [MEDIUM]: {}", logMessage);
            case LOW -> log.info("SECURITY_ALERT [LOW]: {}", logMessage);
        }
    }

    /**
     * Send alert to Slack webhook.
     */
    private void sendToSlack(SecurityAlert alert) {
        try {
            String payload = buildSlackPayload(alert);

            // Use Java HTTP client to send to Slack
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(slackWebhook))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                .build();

            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        log.warn("Failed to send Slack alert: HTTP {}", response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    log.warn("Error sending Slack alert: {}", e.getMessage());
                    return null;
                });

            log.debug("Security alert sent to Slack");
        } catch (Exception e) {
            log.warn("Failed to send Slack alert: {}", e.getMessage());
        }
    }

    /**
     * Send alert via email.
     */
    private void sendToEmail(SecurityAlert alert) {
        try {
            String subject = String.format("[Vokabelnetz Security] %s - %s",
                alert.getSeverity(), alert.getTitle());
            String body = formatAlertForEmail(alert);

            emailService.sendSecurityAlertEmail(alertEmail, subject, body);
            log.debug("Security alert sent to email: {}", maskEmail(alertEmail));
        } catch (Exception e) {
            log.warn("Failed to send email alert: {}", e.getMessage());
        }
    }

    /**
     * Format alert for log output.
     */
    private String formatAlertForLog(SecurityAlert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(alert.getType());
        sb.append(", title=").append(alert.getTitle());
        sb.append(", message=").append(alert.getMessage());

        if (alert.getUserId() != null) {
            sb.append(", userId=").append(alert.getUserId());
        }
        if (alert.getUserEmail() != null) {
            sb.append(", email=").append(alert.getUserEmail());
        }
        if (alert.getIpAddress() != null) {
            sb.append(", ip=").append(alert.getIpAddress());
        }
        if (alert.getMetadata() != null && !alert.getMetadata().isEmpty()) {
            sb.append(", metadata=").append(alert.getMetadata());
        }

        return sb.toString();
    }

    /**
     * Build Slack message payload.
     */
    private String buildSlackPayload(SecurityAlert alert) {
        String color = switch (alert.getSeverity()) {
            case CRITICAL -> "#FF0000"; // Red
            case HIGH -> "#FF8C00"; // Dark Orange
            case MEDIUM -> "#FFA500"; // Orange
            case LOW -> "#FFFF00"; // Yellow
        };

        StringBuilder fields = new StringBuilder();
        if (alert.getUserEmail() != null) {
            fields.append(String.format("{\"title\": \"Email\", \"value\": \"%s\", \"short\": true},",
                alert.getUserEmail()));
        }
        if (alert.getIpAddress() != null) {
            fields.append(String.format("{\"title\": \"IP Address\", \"value\": \"%s\", \"short\": true},",
                alert.getIpAddress()));
        }
        fields.append(String.format("{\"title\": \"Time\", \"value\": \"%s\", \"short\": true}",
            alert.getTimestamp().toString()));

        return String.format("""
            {
                "attachments": [{
                    "color": "%s",
                    "title": "[%s] %s",
                    "text": "%s",
                    "fields": [%s],
                    "footer": "Vokabelnetz Security",
                    "ts": %d
                }]
            }
            """, color, alert.getSeverity(), alert.getTitle(),
            alert.getMessage().replace("\"", "\\\""),
            fields.toString(),
            alert.getTimestamp().getEpochSecond());
    }

    /**
     * Format alert for email body.
     */
    private String formatAlertForEmail(SecurityAlert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(alert.getTitle()).append("</h2>");
        sb.append("<p><strong>Severity:</strong> ").append(alert.getSeverity()).append("</p>");
        sb.append("<p><strong>Type:</strong> ").append(alert.getType()).append("</p>");
        sb.append("<p><strong>Message:</strong> ").append(alert.getMessage()).append("</p>");
        sb.append("<hr>");
        sb.append("<h3>Details</h3>");
        sb.append("<ul>");

        if (alert.getUserId() != null) {
            sb.append("<li><strong>User ID:</strong> ").append(alert.getUserId()).append("</li>");
        }
        if (alert.getUserEmail() != null) {
            sb.append("<li><strong>Email:</strong> ").append(alert.getUserEmail()).append("</li>");
        }
        if (alert.getIpAddress() != null) {
            sb.append("<li><strong>IP Address:</strong> ").append(alert.getIpAddress()).append("</li>");
        }
        if (alert.getUserAgent() != null) {
            sb.append("<li><strong>User Agent:</strong> ").append(alert.getUserAgent()).append("</li>");
        }
        sb.append("<li><strong>Timestamp:</strong> ").append(alert.getTimestamp()).append("</li>");

        if (alert.getMetadata() != null && !alert.getMetadata().isEmpty()) {
            sb.append("<li><strong>Additional Info:</strong><ul>");
            alert.getMetadata().forEach((k, v) ->
                sb.append("<li>").append(k).append(": ").append(v).append("</li>"));
            sb.append("</ul></li>");
        }

        sb.append("</ul>");
        return sb.toString();
    }

    /**
     * Mask email for privacy in logs.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "[INVALID]";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    /**
     * Security alert data structure.
     */
    @lombok.Builder
    @lombok.Data
    public static class SecurityAlert {
        private AlertType type;
        private AlertSeverity severity;
        private String title;
        private String message;
        private Long userId;
        private String userEmail;
        private String ipAddress;
        private String userAgent;
        private Map<String, String> metadata;
        private Instant timestamp;
    }
}

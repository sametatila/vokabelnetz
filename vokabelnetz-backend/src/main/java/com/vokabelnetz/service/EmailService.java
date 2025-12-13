package com.vokabelnetz.service;

import com.vokabelnetz.config.MailProperties;
import com.vokabelnetz.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email service for sending various notifications.
 * Based on SECURITY.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * Send password reset email.
     */
    @Async
    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetUrl = mailProperties.getFrontendUrl() + "/auth/reset-password?token=" + resetToken;

        String subject = "Vokabelnetz - Password Reset Request";
        String body = buildPasswordResetEmailBody(user.getDisplayName(), resetUrl);

        sendEmail(user.getEmail(), subject, body);
        log.info("Password reset email sent to user: {}", user.getId());
    }

    /**
     * Send password changed notification.
     */
    @Async
    public void sendPasswordChangedNotification(User user) {
        String subject = "Vokabelnetz - Your Password Was Changed";
        String body = buildPasswordChangedEmailBody(user.getDisplayName());

        sendEmail(user.getEmail(), subject, body);
        log.info("Password changed notification sent to user: {}", user.getId());
    }

    /**
     * Send email change notification (to old email).
     */
    @Async
    public void sendEmailChangedNotification(String oldEmail, String newEmail) {
        String subject = "Vokabelnetz - Your Email Address Was Changed";
        String body = buildEmailChangedEmailBody(newEmail);

        sendEmail(oldEmail, subject, body);
        log.info("Email changed notification sent to: {}", maskEmail(oldEmail));
    }

    /**
     * Send account deleted notification.
     */
    @Async
    public void sendAccountDeletedNotification(User user) {
        String subject = "Vokabelnetz - Account Deletion Confirmation";
        String body = buildAccountDeletedEmailBody(user.getDisplayName());

        sendEmail(user.getEmail(), subject, body);
        log.info("Account deletion notification sent to user: {}", user.getId());
    }

    /**
     * Send welcome email after registration.
     */
    @Async
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to Vokabelnetz!";
        String body = buildWelcomeEmailBody(user.getDisplayName());

        sendEmail(user.getEmail(), subject, body);
        log.info("Welcome email sent to user: {}", user.getId());
    }

    /**
     * Send email verification email.
     */
    @Async
    public void sendEmailVerificationEmail(User user, String verificationToken) {
        String verifyUrl = mailProperties.getFrontendUrl() + "/auth/verify-email?token=" + verificationToken;

        String subject = "Vokabelnetz - Verify Your Email Address";
        String body = buildEmailVerificationEmailBody(user.getDisplayName(), verifyUrl);

        sendEmail(user.getEmail(), subject, body);
        log.info("Email verification email sent to user: {}", user.getId());
    }

    /**
     * Send security alert email to admin.
     */
    @Async
    public void sendSecurityAlertEmail(String adminEmail, String subject, String htmlBody) {
        String fullBody = buildSecurityAlertEmailWrapper(htmlBody);
        sendEmail(adminEmail, subject, fullBody);
        log.info("Security alert email sent to admin");
    }

    /**
     * Send streak reminder email.
     */
    @Async
    public void sendStreakReminderEmail(User user, int currentStreak) {
        String subject = "Vokabelnetz - Don't Lose Your " + currentStreak + "-Day Streak!";
        String body = buildStreakReminderEmailBody(user.getDisplayName(), currentStreak);

        sendEmail(user.getEmail(), subject, body);
        log.info("Streak reminder email sent to user: {}", user.getId());
    }

    /**
     * Send weekly progress report.
     */
    @Async
    public void sendWeeklyReportEmail(User user, int wordsLearned, int wordsReviewed, int currentStreak) {
        String subject = "Vokabelnetz - Your Weekly Progress Report";
        String body = buildWeeklyReportEmailBody(user.getDisplayName(), wordsLearned, wordsReviewed, currentStreak);

        sendEmail(user.getEmail(), subject, body);
        log.info("Weekly report email sent to user: {}", user.getId());
    }

    /**
     * Core email sending method.
     */
    private void sendEmail(String to, String subject, String htmlBody) {
        if (!mailProperties.isEnabled()) {
            log.info("Email sending disabled. Would send to: {}, subject: {}", maskEmail(to), subject);
            log.debug("Email body:\n{}", htmlBody);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.debug("Email sent successfully to: {}", maskEmail(to));

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", maskEmail(to), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage());
        }
    }

    // ==================== Email Templates ====================

    private String buildPasswordResetEmailBody(String displayName, String resetUrl) {
        String name = displayName != null ? displayName : "there";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Password Reset Request</h2>
                    <p>Hello %s,</p>
                    <p>We received a request to reset your password. Click the button below to set a new password:</p>
                    <a href="%s" class="button">Reset Password</a>
                    <p>This link will expire in <strong>1 hour</strong>.</p>
                    <p>If you didn't request this, please ignore this email or contact support if you're concerned.</p>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                        <p>If the button doesn't work, copy and paste this link: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, resetUrl, resetUrl);
    }

    private String buildPasswordChangedEmailBody(String displayName) {
        String name = displayName != null ? displayName : "there";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .warning { background-color: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Password Changed Successfully</h2>
                    <p>Hello %s,</p>
                    <p>Your password was changed successfully. All your active sessions have been logged out for security.</p>
                    <div class="warning">
                        <strong>Didn't make this change?</strong><br>
                        If you didn't change your password, please contact our support team immediately.
                    </div>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name);
    }

    private String buildEmailChangedEmailBody(String newEmail) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .warning { background-color: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Email Address Changed</h2>
                    <p>Hello,</p>
                    <p>The email address associated with your Vokabelnetz account has been changed to: <strong>%s</strong></p>
                    <div class="warning">
                        <strong>Didn't make this change?</strong><br>
                        If you didn't change your email address, please contact our support team immediately.
                    </div>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(newEmail);
    }

    private String buildAccountDeletedEmailBody(String displayName) {
        String name = displayName != null ? displayName : "there";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .info { background-color: #e7f3ff; border: 1px solid #0066cc; padding: 15px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Account Deletion Confirmation</h2>
                    <p>Hello %s,</p>
                    <p>Your Vokabelnetz account has been successfully deleted as requested.</p>
                    <div class="info">
                        <strong>Recovery Period:</strong><br>
                        Your data will be permanently deleted after 30 days. If you change your mind, please contact support before then.
                    </div>
                    <p>Thank you for using Vokabelnetz. We hope to see you again!</p>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name);
    }

    private String buildWelcomeEmailBody(String displayName) {
        String name = displayName != null ? displayName : "there";
        String loginUrl = mailProperties.getFrontendUrl() + "/auth/login";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .tips { background-color: #f0f7ff; padding: 15px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Welcome to Vokabelnetz!</h2>
                    <p>Hello %s,</p>
                    <p>Welcome to Vokabelnetz! We're excited to help you learn German vocabulary.</p>
                    <a href="%s" class="button">Start Learning</a>
                    <div class="tips">
                        <strong>Quick Tips:</strong>
                        <ul>
                            <li>Set a daily goal to build consistency</li>
                            <li>Practice every day to maintain your streak</li>
                            <li>Words adapt to your level automatically</li>
                        </ul>
                    </div>
                    <p>Viel Erfolg beim Lernen! (Good luck learning!)</p>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, loginUrl);
    }

    private String buildStreakReminderEmailBody(String displayName, int currentStreak) {
        String name = displayName != null ? displayName : "there";
        String practiceUrl = mailProperties.getFrontendUrl() + "/learn";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #ff9800; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .streak { font-size: 48px; color: #ff9800; text-align: center; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Don't Break Your Streak!</h2>
                    <p>Hello %s,</p>
                    <p>You haven't practiced today yet. Don't lose your progress!</p>
                    <div class="streak">%d days</div>
                    <p>Keep your streak alive with just a few minutes of practice.</p>
                    <a href="%s" class="button">Practice Now</a>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                        <p><small>To unsubscribe from streak reminders, update your notification preferences.</small></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, currentStreak, practiceUrl);
    }

    private String buildWeeklyReportEmailBody(String displayName, int wordsLearned, int wordsReviewed, int currentStreak) {
        String name = displayName != null ? displayName : "there";
        String dashboardUrl = mailProperties.getFrontendUrl() + "/dashboard";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .stats { display: flex; justify-content: space-around; margin: 20px 0; }
                    .stat { text-align: center; padding: 15px; background-color: #f5f5f5; border-radius: 8px; min-width: 100px; }
                    .stat-number { font-size: 32px; font-weight: bold; color: #4CAF50; }
                    .stat-label { font-size: 12px; color: #666; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Your Weekly Progress Report</h2>
                    <p>Hello %s,</p>
                    <p>Here's your learning summary for this week:</p>
                    <table style="width: 100%%; margin: 20px 0;">
                        <tr>
                            <td style="text-align: center; padding: 15px; background-color: #f5f5f5; border-radius: 8px;">
                                <div style="font-size: 32px; font-weight: bold; color: #4CAF50;">%d</div>
                                <div style="font-size: 12px; color: #666;">New Words</div>
                            </td>
                            <td style="width: 20px;"></td>
                            <td style="text-align: center; padding: 15px; background-color: #f5f5f5; border-radius: 8px;">
                                <div style="font-size: 32px; font-weight: bold; color: #2196F3;">%d</div>
                                <div style="font-size: 12px; color: #666;">Reviews</div>
                            </td>
                            <td style="width: 20px;"></td>
                            <td style="text-align: center; padding: 15px; background-color: #f5f5f5; border-radius: 8px;">
                                <div style="font-size: 32px; font-weight: bold; color: #ff9800;">%d</div>
                                <div style="font-size: 12px; color: #666;">Day Streak</div>
                            </td>
                        </tr>
                    </table>
                    <p>Keep up the great work!</p>
                    <a href="%s" class="button">View Dashboard</a>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                        <p><small>To unsubscribe from weekly reports, update your notification preferences.</small></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, wordsLearned, wordsReviewed, currentStreak, dashboardUrl);
    }

    private String buildEmailVerificationEmailBody(String displayName, String verifyUrl) {
        String name = displayName != null ? displayName : "there";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .info { background-color: #e7f3ff; border: 1px solid #0066cc; padding: 15px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Verify Your Email Address</h2>
                    <p>Hello %s,</p>
                    <p>Thank you for registering with Vokabelnetz! Please verify your email address by clicking the button below:</p>
                    <a href="%s" class="button">Verify Email</a>
                    <div class="info">
                        <strong>Note:</strong> This link will expire in <strong>7 days</strong>.
                    </div>
                    <p>If you didn't create an account with Vokabelnetz, please ignore this email.</p>
                    <div class="footer">
                        <p>- Vokabelnetz Team</p>
                        <p>If the button doesn't work, copy and paste this link: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, verifyUrl, verifyUrl);
    }

    private String buildSecurityAlertEmailWrapper(String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .alert-box { background-color: #fff3cd; border: 2px solid #ff6b6b; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; border-top: 1px solid #ddd; padding-top: 20px; }
                    h2 { color: #d63031; }
                    h3 { color: #2d3436; }
                    ul { padding-left: 20px; }
                    li { margin-bottom: 8px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="alert-box">
                        %s
                    </div>
                    <div class="footer">
                        <p>This is an automated security alert from Vokabelnetz.</p>
                        <p>Please investigate this event promptly.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(content);
    }

    /**
     * Mask email for logging (privacy).
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
}

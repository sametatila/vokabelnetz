package com.vokabelnetz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mail configuration properties.
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {

    /**
     * Whether email sending is enabled.
     * If false, emails will be logged but not sent.
     */
    private boolean enabled = false;

    /**
     * From address for emails.
     */
    private String fromAddress = "noreply@vokabelnetz.com";

    /**
     * From name for emails.
     */
    private String fromName = "Vokabelnetz";

    /**
     * Frontend base URL for links in emails.
     */
    private String frontendUrl = "http://localhost:4200";
}

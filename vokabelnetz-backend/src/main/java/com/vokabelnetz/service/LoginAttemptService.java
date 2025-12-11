package com.vokabelnetz.service;

import com.vokabelnetz.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Login attempt tracking service for brute force protection.
 * Based on SECURITY.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final AppProperties appProperties;

    // In-memory cache (use Redis in production for distributed deployments)
    private final Map<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    /**
     * Record a failed login attempt.
     */
    public void recordFailedAttempt(String email, String ipAddress) {
        String key = createKey(email, ipAddress);
        var config = appProperties.getSecurity();

        AttemptInfo info = attemptsCache.compute(key, (k, existing) -> {
            if (existing == null) {
                return new AttemptInfo(1, Instant.now());
            }
            return new AttemptInfo(existing.attempts + 1, Instant.now());
        });

        if (info.attempts >= config.getMaxLoginAttempts()) {
            log.warn("SECURITY: Account locked due to {} failed attempts. Key: {}",
                info.attempts, key);
        }
    }

    /**
     * Check if login is allowed.
     */
    public boolean isLoginAllowed(String email, String ipAddress) {
        String key = createKey(email, ipAddress);
        var config = appProperties.getSecurity();

        AttemptInfo info = attemptsCache.get(key);
        if (info == null) {
            return true;
        }

        // Check if lockout period has passed
        Instant lockoutEnd = info.lastAttempt.plusSeconds(config.getLockoutMinutes() * 60L);
        if (Instant.now().isAfter(lockoutEnd)) {
            attemptsCache.remove(key);
            return true;
        }

        return info.attempts < config.getMaxLoginAttempts();
    }

    /**
     * Record a successful login (clear attempts).
     */
    public void recordSuccessfulLogin(String email, String ipAddress) {
        String key = createKey(email, ipAddress);
        attemptsCache.remove(key);
    }

    private String createKey(String email, String ipAddress) {
        return email.toLowerCase() + ":" + ipAddress;
    }

    private record AttemptInfo(int attempts, Instant lastAttempt) {}
}

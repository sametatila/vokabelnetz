package com.vokabelnetz.service;

import com.vokabelnetz.entity.PasswordHistory;
import com.vokabelnetz.entity.User;
import com.vokabelnetz.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing password history.
 * Prevents users from reusing recent passwords.
 * Based on SECURITY.md documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordHistoryService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Number of previous passwords to check against.
     * User cannot reuse any of the last N passwords.
     */
    private static final int PASSWORD_HISTORY_COUNT = 5;

    /**
     * Check if a password was used before by this user.
     *
     * @param user The user
     * @param rawPassword The raw password to check
     * @return true if the password was used before
     */
    @Transactional(readOnly = true)
    public boolean isPasswordUsedBefore(User user, String rawPassword) {
        List<PasswordHistory> recentPasswords = passwordHistoryRepository
            .findRecentByUserId(user.getId(), PASSWORD_HISTORY_COUNT);

        for (PasswordHistory history : recentPasswords) {
            if (passwordEncoder.matches(rawPassword, history.getPasswordHash())) {
                log.debug("Password reuse detected for user: {}", user.getId());
                return true;
            }
        }

        return false;
    }

    /**
     * Save a password hash to history.
     * Call this after successfully changing a password.
     *
     * @param user The user
     * @param passwordHash The already-hashed password
     */
    @Transactional
    public void savePasswordHash(User user, String passwordHash) {
        PasswordHistory history = PasswordHistory.builder()
            .user(user)
            .passwordHash(passwordHash)
            .build();

        passwordHistoryRepository.save(history);

        // Clean up old entries, keeping only the most recent ones
        long count = passwordHistoryRepository.countByUserId(user.getId());
        if (count > PASSWORD_HISTORY_COUNT) {
            passwordHistoryRepository.deleteOldEntries(user.getId(), PASSWORD_HISTORY_COUNT);
            log.debug("Cleaned up old password history for user: {}", user.getId());
        }

        log.debug("Saved password to history for user: {}", user.getId());
    }

    /**
     * Delete all password history for a user.
     * Call this when deleting a user account.
     *
     * @param userId The user ID
     */
    @Transactional
    public void deleteAllForUser(Long userId) {
        passwordHistoryRepository.deleteByUserId(userId);
        log.debug("Deleted all password history for user: {}", userId);
    }

    /**
     * Get the number of passwords kept in history.
     */
    public int getPasswordHistoryCount() {
        return PASSWORD_HISTORY_COUNT;
    }
}

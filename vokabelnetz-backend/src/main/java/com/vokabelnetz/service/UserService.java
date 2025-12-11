package com.vokabelnetz.service;

import com.vokabelnetz.entity.User;
import com.vokabelnetz.entity.UserPreferences;
import com.vokabelnetz.exception.ResourceNotFoundException;
import com.vokabelnetz.repository.UserPreferencesRepository;
import com.vokabelnetz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User management service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;

    /**
     * Find user by ID.
     */
    public User findById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * Find user by email.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email);
    }

    /**
     * Check if email exists.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    /**
     * Save user.
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Get or create user preferences.
     */
    @Transactional
    public UserPreferences getOrCreatePreferences(User user) {
        return preferencesRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                UserPreferences preferences = UserPreferences.builder()
                    .user(user)
                    .build();
                return preferencesRepository.save(preferences);
            });
    }

    /**
     * Update user preferences.
     */
    @Transactional
    public UserPreferences updatePreferences(Long userId, UserPreferences updatedPreferences) {
        UserPreferences existing = preferencesRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("UserPreferences not found"));

        // Update fields
        if (updatedPreferences.getEmailNotifications() != null) {
            existing.setEmailNotifications(updatedPreferences.getEmailNotifications());
        }
        if (updatedPreferences.getStreakReminders() != null) {
            existing.setStreakReminders(updatedPreferences.getStreakReminders());
        }
        if (updatedPreferences.getWeeklyReport() != null) {
            existing.setWeeklyReport(updatedPreferences.getWeeklyReport());
        }
        if (updatedPreferences.getAutoPlayAudio() != null) {
            existing.setAutoPlayAudio(updatedPreferences.getAutoPlayAudio());
        }
        if (updatedPreferences.getShowExampleSentences() != null) {
            existing.setShowExampleSentences(updatedPreferences.getShowExampleSentences());
        }
        if (updatedPreferences.getWordsPerSession() != null) {
            existing.setWordsPerSession(updatedPreferences.getWordsPerSession());
        }
        if (updatedPreferences.getDarkMode() != null) {
            existing.setDarkMode(updatedPreferences.getDarkMode());
        }

        return preferencesRepository.save(existing);
    }
}

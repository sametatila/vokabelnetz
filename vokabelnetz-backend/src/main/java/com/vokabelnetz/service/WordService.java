package com.vokabelnetz.service;

import com.vokabelnetz.entity.Word;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.WordCategory;
import com.vokabelnetz.exception.ResourceNotFoundException;
import com.vokabelnetz.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Word management service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;
    private final Random random = new Random();

    /**
     * Find word by ID.
     */
    public Word findById(Long id) {
        return wordRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Word", id));
    }

    /**
     * Get all words with pagination.
     */
    public Page<Word> findAll(Pageable pageable) {
        return wordRepository.findByIsActiveTrue(pageable);
    }

    /**
     * Get words by CEFR level.
     */
    public List<Word> findByCefrLevel(CefrLevel level) {
        return wordRepository.findByCefrLevelAndIsActiveTrue(level);
    }

    /**
     * Get words by category.
     */
    public List<Word> findByCategory(WordCategory category) {
        return wordRepository.findByCategoryAndIsActiveTrue(category);
    }

    /**
     * Search words by German term.
     */
    public Page<Word> searchByGerman(String searchTerm, Pageable pageable) {
        return wordRepository.searchByGerman(searchTerm, pageable);
    }

    /**
     * Get new words for user to learn.
     */
    public List<Word> findNewWordsForUser(Long userId, CefrLevel cefrLevel, Pageable pageable) {
        return wordRepository.findNewWordsForUser(userId, cefrLevel, pageable);
    }

    /**
     * Get words within Elo rating range.
     */
    public List<Word> findByDifficultyRange(int minRating, int maxRating, CefrLevel cefrLevel) {
        return wordRepository.findByDifficultyRatingRange(minRating, maxRating, cefrLevel);
    }

    /**
     * Get a random word, optionally filtered by CEFR level.
     */
    public Word findRandom(CefrLevel cefrLevel) {
        List<Word> words;
        if (cefrLevel != null) {
            words = wordRepository.findByCefrLevelAndIsActiveTrue(cefrLevel);
        } else {
            words = wordRepository.findAll().stream()
                .filter(Word::getIsActive)
                .toList();
        }
        if (words.isEmpty()) {
            throw new ResourceNotFoundException("No words available");
        }
        return words.get(random.nextInt(words.size()));
    }

    /**
     * Get word statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalWords = wordRepository.count();
        stats.put("totalWords", totalWords);

        long activeWords = wordRepository.findAll().stream()
            .filter(Word::getIsActive)
            .count();
        stats.put("activeWords", activeWords);

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (CefrLevel level : CefrLevel.values()) {
            byLevel.put(level.name(), wordRepository.countByCefrLevelAndIsActiveTrue(level));
        }
        stats.put("byLevel", byLevel);

        return stats;
    }

    /**
     * Save word (admin function).
     */
    @Transactional
    public Word save(Word word) {
        return wordRepository.save(word);
    }

    /**
     * Update word's global stats after answer.
     */
    @Transactional
    public void updateStats(Word word, boolean correct) {
        word.setTimesShown(word.getTimesShown() + 1);
        if (correct) {
            word.setTimesCorrect(word.getTimesCorrect() + 1);
        }
        wordRepository.save(word);
    }

    /**
     * Count words by CEFR level.
     */
    public long countByCefrLevel(CefrLevel level) {
        return wordRepository.countByCefrLevelAndIsActiveTrue(level);
    }
}

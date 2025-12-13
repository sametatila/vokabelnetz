package com.vokabelnetz.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vokabelnetz.entity.Word;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.WordCategory;
import com.vokabelnetz.entity.enums.WordType;
import com.vokabelnetz.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Initializes the database with word data from JSON files.
 *
 * Modes:
 * - INIT: Only seed if database is empty
 * - UPDATE: Add new words, update existing ones (by german + cefrLevel)
 * - VALIDATE: Just validate files, no database changes
 * - NONE: Skip data initialization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final WordRepository wordRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String seedMode = appProperties.getData().getSeedMode().toUpperCase();
        log.info("Data initialization mode: {}", seedMode);

        switch (seedMode) {
            case "INIT" -> initMode();
            case "UPDATE" -> updateMode();
            case "VALIDATE" -> validateMode();
            case "NONE" -> log.info("Data initialization skipped (mode: NONE)");
            default -> log.warn("Unknown seed mode: {}. Skipping initialization.", seedMode);
        }
    }

    /**
     * INIT mode: Only seed if database is empty.
     */
    private void initMode() {
        long existingCount = wordRepository.count();
        if (existingCount > 0) {
            log.info("Database already contains {} words. Skipping INIT mode.", existingCount);
            return;
        }

        log.info("Database is empty. Starting initial data seeding...");
        List<Word> words = loadAllWords();
        if (!words.isEmpty()) {
            wordRepository.saveAll(words);
            log.info("Successfully seeded {} words into the database.", words.size());
        }
    }

    /**
     * UPDATE mode: Add new words, update existing ones.
     */
    private void updateMode() {
        log.info("Starting UPDATE mode...");
        List<Word> wordsFromFiles = loadAllWords();

        int added = 0;
        int updated = 0;
        int skipped = 0;

        for (Word newWord : wordsFromFiles) {
            Word existing = wordRepository.findByGermanAndCefrLevel(newWord.getGerman(), newWord.getCefrLevel());

            if (existing == null) {
                wordRepository.save(newWord);
                added++;
            } else {
                // Update existing word with new data
                updateWordFields(existing, newWord);
                wordRepository.save(existing);
                updated++;
            }
        }

        log.info("UPDATE mode completed: {} added, {} updated, {} skipped", added, updated, skipped);
    }

    /**
     * VALIDATE mode: Just validate files, no database changes.
     */
    private void validateMode() {
        log.info("Starting VALIDATE mode...");
        try {
            List<Word> words = loadAllWords();
            log.info("Validation successful: {} words found in JSON files", words.size());

            // Log stats per level
            Map<CefrLevel, Long> levelCounts = words.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Word::getCefrLevel,
                    java.util.stream.Collectors.counting()
                ));
            levelCounts.forEach((level, count) ->
                log.info("  {} level: {} words", level, count)
            );
        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Load all words from JSON files in the data directory.
     */
    private List<Word> loadAllWords() {
        List<Word> allWords = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources("classpath:data/words-*.json");
            log.info("Found {} word data files", resources.length);

            for (Resource resource : resources) {
                log.info("Loading words from: {}", resource.getFilename());
                List<Word> words = loadWordsFromResource(resource);
                allWords.addAll(words);
                log.info("  Loaded {} words from {}", words.size(), resource.getFilename());
            }
        } catch (IOException e) {
            log.error("Error loading word files: {}", e.getMessage(), e);
        }

        return allWords;
    }

    /**
     * Load words from a single JSON resource file.
     */
    private List<Word> loadWordsFromResource(Resource resource) {
        List<Word> words = new ArrayList<>();

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);

            // Get metadata for CEFR level
            JsonNode metadata = root.get("metadata");
            String levelStr = metadata != null && metadata.has("level")
                ? metadata.get("level").asText()
                : extractLevelFromFilename(resource.getFilename());
            CefrLevel cefrLevel = CefrLevel.valueOf(levelStr);

            String source = metadata != null && metadata.has("source")
                ? metadata.get("source").asText()
                : "Unknown";

            // Parse words array
            JsonNode wordsArray = root.get("words");
            if (wordsArray != null && wordsArray.isArray()) {
                for (JsonNode wordNode : wordsArray) {
                    Word word = parseWordNode(wordNode, cefrLevel, source);
                    if (word != null) {
                        words.add(word);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading file {}: {}", resource.getFilename(), e.getMessage());
        }

        return words;
    }

    /**
     * Parse a single word JSON node into a Word entity.
     */
    private Word parseWordNode(JsonNode node, CefrLevel defaultLevel, String source) {
        try {
            String german = node.get("german").asText();

            // Parse translations to JSON string
            String translations = node.has("translations")
                ? objectMapper.writeValueAsString(node.get("translations"))
                : null;

            // Parse example sentences to JSON string
            String exampleSentences = node.has("examples")
                ? objectMapper.writeValueAsString(node.get("examples"))
                : null;

            // Parse tags to JSON string
            String tags = node.has("tags")
                ? objectMapper.writeValueAsString(node.get("tags"))
                : null;

            // Parse word type
            WordType wordType = null;
            if (node.has("wordType") && !node.get("wordType").isNull()) {
                try {
                    wordType = WordType.valueOf(node.get("wordType").asText());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown word type: {} for word: {}",
                        node.get("wordType").asText(), german);
                }
            }

            // Parse category
            WordCategory category = null;
            if (node.has("category") && !node.get("category").isNull()) {
                try {
                    category = WordCategory.valueOf(node.get("category").asText());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown category: {} for word: {}",
                        node.get("category").asText(), german);
                }
            }

            // Parse CEFR level (use default if not specified)
            CefrLevel level = defaultLevel;
            if (node.has("cefrLevel") && !node.get("cefrLevel").isNull()) {
                try {
                    level = CefrLevel.valueOf(node.get("cefrLevel").asText());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown CEFR level: {} for word: {}, using default: {}",
                        node.get("cefrLevel").asText(), german, defaultLevel);
                }
            }

            // Parse initial difficulty
            int difficulty = 1000;
            if (node.has("difficulty") && node.get("difficulty").has("initial")) {
                difficulty = node.get("difficulty").get("initial").asInt(1000);
            }

            return Word.builder()
                .german(german)
                .article(getTextOrNull(node, "article"))
                .plural(getTextOrNull(node, "plural"))
                .translations(translations)
                .wordType(wordType)
                .cefrLevel(level)
                .category(category)
                .exampleSentences(exampleSentences)
                .difficultyRating(difficulty)
                .tags(tags)
                .source(source)
                .isActive(true)
                .timesShown(0L)
                .timesCorrect(0L)
                .build();

        } catch (Exception e) {
            log.error("Error parsing word node: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract CEFR level from filename (e.g., "words-a1.json" -> "A1").
     */
    private String extractLevelFromFilename(String filename) {
        if (filename == null) return "A1";

        // Pattern: words-a1.json, words-a2-sample.json, etc.
        String lower = filename.toLowerCase();
        if (lower.contains("-a1")) return "A1";
        if (lower.contains("-a2")) return "A2";
        if (lower.contains("-b1")) return "B1";
        if (lower.contains("-b2")) return "B2";
        if (lower.contains("-c1")) return "C1";
        if (lower.contains("-c2")) return "C2";

        return "A1"; // Default
    }

    /**
     * Get text value from JSON node or null if not present.
     */
    private String getTextOrNull(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    /**
     * Update existing word fields with new data.
     */
    private void updateWordFields(Word existing, Word newWord) {
        // Update basic fields
        if (newWord.getArticle() != null) {
            existing.setArticle(newWord.getArticle());
        }
        if (newWord.getPlural() != null) {
            existing.setPlural(newWord.getPlural());
        }
        if (newWord.getTranslations() != null) {
            existing.setTranslations(newWord.getTranslations());
        }
        if (newWord.getWordType() != null) {
            existing.setWordType(newWord.getWordType());
        }
        if (newWord.getCategory() != null) {
            existing.setCategory(newWord.getCategory());
        }
        if (newWord.getExampleSentences() != null) {
            existing.setExampleSentences(newWord.getExampleSentences());
        }
        if (newWord.getTags() != null) {
            existing.setTags(newWord.getTags());
        }
        if (newWord.getSource() != null) {
            existing.setSource(newWord.getSource());
        }
        // Don't update difficulty rating or stats - preserve user-generated data
    }
}

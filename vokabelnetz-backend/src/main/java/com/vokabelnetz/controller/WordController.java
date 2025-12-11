package com.vokabelnetz.controller;

import com.vokabelnetz.dto.response.ApiResponse;
import com.vokabelnetz.dto.response.MetaData;
import com.vokabelnetz.entity.Word;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.WordCategory;
import com.vokabelnetz.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Word management controller.
 * Based on API.md documentation.
 */
@RestController
@RequestMapping("/words")
@RequiredArgsConstructor
@Tag(name = "Words", description = "Word management endpoints")
public class WordController {

    private final WordService wordService;

    /**
     * Get all words with pagination.
     * GET /api/words?page=0&size=20&sort=german
     */
    @GetMapping
    @Operation(summary = "Get all words", description = "Get paginated list of words")
    public ResponseEntity<ApiResponse<List<Word>>> getWords(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "german") String sort
    ) {
        Page<Word> wordPage = wordService.findAll(
            PageRequest.of(page, size, Sort.by(sort))
        );

        return ResponseEntity.ok(ApiResponse.success(
            wordPage.getContent(),
            MetaData.of(page, size, wordPage.getTotalElements())
        ));
    }

    /**
     * Get single word by ID.
     * GET /api/words/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get word by ID", description = "Get a single word by its ID")
    public ResponseEntity<ApiResponse<Word>> getWord(@PathVariable Long id) {
        Word word = wordService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(word));
    }

    /**
     * Get words by CEFR level.
     * GET /api/words/level/{level}
     */
    @GetMapping("/level/{level}")
    @Operation(summary = "Get words by level", description = "Get all words for a specific CEFR level")
    public ResponseEntity<ApiResponse<List<Word>>> getWordsByLevel(
        @PathVariable CefrLevel level
    ) {
        List<Word> words = wordService.findByCefrLevel(level);
        return ResponseEntity.ok(ApiResponse.success(words));
    }

    /**
     * Get words by category.
     * GET /api/words/category/{category}
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get words by category", description = "Get all words for a specific category")
    public ResponseEntity<ApiResponse<List<Word>>> getWordsByCategory(
        @PathVariable WordCategory category
    ) {
        List<Word> words = wordService.findByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(words));
    }

    /**
     * Search words by German term.
     * GET /api/words/search?q=arbeit
     */
    @GetMapping("/search")
    @Operation(summary = "Search words", description = "Search words by German term")
    public ResponseEntity<ApiResponse<List<Word>>> searchWords(
        @RequestParam("q") String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<Word> wordPage = wordService.searchByGerman(
            query,
            PageRequest.of(page, size)
        );

        return ResponseEntity.ok(ApiResponse.success(
            wordPage.getContent(),
            MetaData.of(page, size, wordPage.getTotalElements())
        ));
    }

    /**
     * Get a random word.
     * GET /api/words/random?level=A1
     */
    @GetMapping("/random")
    @Operation(summary = "Get random word", description = "Get a random word, optionally filtered by level")
    public ResponseEntity<ApiResponse<Word>> getRandomWord(
        @RequestParam(required = false) CefrLevel level
    ) {
        Word word = wordService.findRandom(level);
        return ResponseEntity.ok(ApiResponse.success(word));
    }

    /**
     * Get word statistics.
     * GET /api/words/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get word statistics", description = "Get overall word statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWordStats() {
        Map<String, Object> stats = wordService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

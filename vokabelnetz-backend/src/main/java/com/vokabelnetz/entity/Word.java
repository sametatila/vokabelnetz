package com.vokabelnetz.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.WordCategory;
import com.vokabelnetz.entity.enums.WordType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Word entity representing German vocabulary items.
 */
@Entity
@Table(name = "words", indexes = {
    @Index(name = "idx_words_cefr_level", columnList = "cefr_level"),
    @Index(name = "idx_words_category", columnList = "category"),
    @Index(name = "idx_words_difficulty_rating", columnList = "difficulty_rating")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String german;

    @Column(length = 10)
    private String article;

    @Column(length = 200)
    private String plural;

    // Translations stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String translations;

    @Enumerated(EnumType.STRING)
    @Column(name = "word_type", length = 20)
    private WordType wordType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false, length = 2)
    private CefrLevel cefrLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WordCategory category;

    // Example sentences stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "example_sentences", columnDefinition = "jsonb")
    private String exampleSentences;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Elo-based difficulty rating
    @Builder.Default
    @Column(name = "difficulty_rating")
    private Integer difficultyRating = 1000;

    // Global stats
    @Builder.Default
    @Column(name = "times_shown")
    private Long timesShown = 0L;

    @Builder.Default
    @Column(name = "times_correct")
    private Long timesCorrect = 0L;

    // Active status
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    // Source tracking
    @Column(length = 100)
    private String source;

    // Tags stored as JSONB array
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tags;

    // Relationships - excluded from JSON to prevent lazy loading issues
    @JsonIgnore
    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserWordProgress> userProgress = new ArrayList<>();

    /**
     * Calculate global success rate.
     */
    public double getGlobalSuccessRate() {
        if (timesShown == null || timesShown == 0) {
            return 0.0;
        }
        return (double) (timesCorrect != null ? timesCorrect : 0) / timesShown;
    }
}

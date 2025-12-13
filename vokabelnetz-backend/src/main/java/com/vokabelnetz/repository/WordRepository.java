package com.vokabelnetz.repository;

import com.vokabelnetz.entity.Word;
import com.vokabelnetz.entity.enums.CefrLevel;
import com.vokabelnetz.entity.enums.WordCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByCefrLevelAndIsActiveTrue(CefrLevel cefrLevel);

    List<Word> findByCategoryAndIsActiveTrue(WordCategory category);

    Page<Word> findByIsActiveTrue(Pageable pageable);

    /**
     * Find new words for a user (words they haven't started learning).
     */
    @Query("""
        SELECT w FROM Word w
        WHERE w.isActive = true
        AND w.cefrLevel = :cefrLevel
        AND w.id NOT IN (
            SELECT uwp.word.id FROM UserWordProgress uwp WHERE uwp.user.id = :userId
        )
        ORDER BY w.difficultyRating ASC
        """)
    List<Word> findNewWordsForUser(
        @Param("userId") Long userId,
        @Param("cefrLevel") CefrLevel cefrLevel,
        Pageable pageable
    );

    /**
     * Find words within Elo rating range.
     */
    @Query("""
        SELECT w FROM Word w
        WHERE w.isActive = true
        AND w.difficultyRating BETWEEN :minRating AND :maxRating
        AND w.cefrLevel = :cefrLevel
        """)
    List<Word> findByDifficultyRatingRange(
        @Param("minRating") int minRating,
        @Param("maxRating") int maxRating,
        @Param("cefrLevel") CefrLevel cefrLevel
    );

    /**
     * Count words by CEFR level (active).
     */
    long countByCefrLevelAndIsActiveTrue(CefrLevel cefrLevel);

    /**
     * Count all words by CEFR level.
     */
    long countByCefrLevel(CefrLevel cefrLevel);

    /**
     * Search words by German term.
     */
    @Query("""
        SELECT w FROM Word w
        WHERE w.isActive = true
        AND LOWER(w.german) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        """)
    Page<Word> searchByGerman(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find word by German term and CEFR level (for data seeding).
     */
    Word findByGermanAndCefrLevel(String german, CefrLevel cefrLevel);

    /**
     * Check if word exists by German term and CEFR level.
     */
    boolean existsByGermanAndCefrLevel(String german, CefrLevel cefrLevel);
}

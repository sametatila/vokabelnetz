package com.vokabelnetz.repository;

import com.vokabelnetz.entity.UserWordProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserWordProgressRepository extends JpaRepository<UserWordProgress, Long> {

    Optional<UserWordProgress> findByUserIdAndWordId(Long userId, Long wordId);

    List<UserWordProgress> findByUserId(Long userId);

    /**
     * Find words due for review (SM-2 scheduling).
     */
    @Query("""
        SELECT uwp FROM UserWordProgress uwp
        WHERE uwp.user.id = :userId
        AND uwp.nextReviewAt <= :now
        ORDER BY uwp.nextReviewAt ASC
        """)
    List<UserWordProgress> findDueForReview(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    /**
     * Count overdue words.
     */
    @Query("""
        SELECT COUNT(uwp) FROM UserWordProgress uwp
        WHERE uwp.user.id = :userId
        AND uwp.nextReviewAt < :now
        """)
    int countOverdue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Find learned words for user.
     */
    List<UserWordProgress> findByUserIdAndIsLearnedTrue(Long userId);

    /**
     * Count learned words for user.
     */
    long countByUserIdAndIsLearnedTrue(Long userId);

    /**
     * Find favorite words for user.
     */
    List<UserWordProgress> findByUserIdAndIsFavoriteTrue(Long userId);

    /**
     * Find difficult words for user.
     */
    List<UserWordProgress> findByUserIdAndIsDifficultTrue(Long userId);

    /**
     * Get words with lowest success rate for user.
     */
    @Query("""
        SELECT uwp FROM UserWordProgress uwp
        WHERE uwp.user.id = :userId
        AND (uwp.timesCorrect + uwp.timesIncorrect) > 0
        ORDER BY (CAST(uwp.timesCorrect AS double) / (uwp.timesCorrect + uwp.timesIncorrect)) ASC
        """)
    List<UserWordProgress> findWordsWithLowestSuccessRate(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find recently reviewed words.
     */
    @Query("""
        SELECT uwp FROM UserWordProgress uwp
        WHERE uwp.user.id = :userId
        AND uwp.lastReviewedAt IS NOT NULL
        ORDER BY uwp.lastReviewedAt DESC
        """)
    Page<UserWordProgress> findRecentlyReviewed(@Param("userId") Long userId, Pageable pageable);
}

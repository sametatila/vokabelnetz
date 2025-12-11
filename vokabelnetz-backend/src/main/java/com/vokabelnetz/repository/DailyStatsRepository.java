package com.vokabelnetz.repository;

import com.vokabelnetz.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {

    Optional<DailyStats> findByUserIdAndStatDate(Long userId, LocalDate statDate);

    /**
     * Check if user was active on a specific date (reviewed at least one word).
     */
    @Query("""
        SELECT CASE WHEN COUNT(ds) > 0 THEN true ELSE false END
        FROM DailyStats ds
        WHERE ds.user.id = :userId
        AND ds.statDate = :date
        AND ds.wordsReviewed > 0
        """)
    boolean existsByUserIdAndStatDateAndWordsReviewedGreaterThan(
        @Param("userId") Long userId,
        @Param("date") LocalDate date,
        @Param("minWords") int minWords
    );

    /**
     * Get stats for date range (for charts).
     */
    @Query("""
        SELECT ds FROM DailyStats ds
        WHERE ds.user.id = :userId
        AND ds.statDate BETWEEN :startDate AND :endDate
        ORDER BY ds.statDate ASC
        """)
    List<DailyStats> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get stats for date range (alternative method name).
     */
    List<DailyStats> findByUserIdAndStatDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Get total stats for user.
     */
    @Query("""
        SELECT
            COALESCE(SUM(ds.wordsReviewed), 0),
            COALESCE(SUM(ds.wordsCorrect), 0),
            COALESCE(SUM(ds.newWordsLearned), 0),
            COALESCE(SUM(ds.totalTimeSeconds), 0)
        FROM DailyStats ds
        WHERE ds.user.id = :userId
        """)
    Object[] getTotalStats(@Param("userId") Long userId);

    /**
     * Count active days for user.
     */
    @Query("""
        SELECT COUNT(ds) FROM DailyStats ds
        WHERE ds.user.id = :userId
        AND ds.wordsReviewed > 0
        """)
    long countActiveDays(@Param("userId") Long userId);

    /**
     * Delete old stats for data retention.
     */
    @Query("DELETE FROM DailyStats ds WHERE ds.statDate < :before")
    int deleteOlderThan(@Param("before") LocalDate before);
}

package com.vokabelnetz.repository;

import com.vokabelnetz.entity.StreakHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StreakHistoryRepository extends JpaRepository<StreakHistory, Long> {

    Optional<StreakHistory> findByUserIdAndStreakDate(Long userId, LocalDate streakDate);

    /**
     * Get streak history for date range (for calendar view).
     */
    @Query("""
        SELECT sh FROM StreakHistory sh
        WHERE sh.user.id = :userId
        AND sh.streakDate BETWEEN :startDate AND :endDate
        ORDER BY sh.streakDate ASC
        """)
    List<StreakHistory> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Count days with streak freeze used.
     */
    long countByUserIdAndFreezeUsedTrue(Long userId);

    /**
     * Get most recent streak history entries.
     */
    @Query("""
        SELECT sh FROM StreakHistory sh
        WHERE sh.user.id = :userId
        ORDER BY sh.streakDate DESC
        """)
    List<StreakHistory> findRecentByUserId(@Param("userId") Long userId);
}

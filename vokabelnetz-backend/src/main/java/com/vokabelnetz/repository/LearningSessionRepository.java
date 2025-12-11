package com.vokabelnetz.repository;

import com.vokabelnetz.entity.LearningSession;
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
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    /**
     * Find active session for user (session without end time).
     */
    Optional<LearningSession> findByUserIdAndEndedAtIsNull(Long userId);

    /**
     * Find recent sessions for user.
     */
    Page<LearningSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    /**
     * Find sessions within date range.
     */
    @Query("""
        SELECT ls FROM LearningSession ls
        WHERE ls.user.id = :userId
        AND ls.startedAt BETWEEN :startDate AND :endDate
        ORDER BY ls.startedAt DESC
        """)
    List<LearningSession> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count completed sessions for user.
     */
    long countByUserIdAndEndedAtIsNotNull(Long userId);

    /**
     * Get average session duration for user.
     */
    @Query("""
        SELECT AVG(ls.totalTimeSeconds) FROM LearningSession ls
        WHERE ls.user.id = :userId
        AND ls.endedAt IS NOT NULL
        """)
    Double getAverageSessionDuration(@Param("userId") Long userId);
}

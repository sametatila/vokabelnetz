package com.vokabelnetz.repository;

import com.vokabelnetz.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * Find all password history entries for a user, ordered by most recent first.
     */
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find the last N password hashes for a user.
     */
    @Query("""
        SELECT ph FROM PasswordHistory ph
        WHERE ph.user.id = :userId
        ORDER BY ph.createdAt DESC
        LIMIT :limit
        """)
    List<PasswordHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Count password history entries for a user.
     */
    long countByUserId(Long userId);

    /**
     * Delete oldest entries keeping only the most recent N entries.
     */
    @Modifying
    @Query(value = """
        DELETE FROM password_history
        WHERE user_id = :userId
        AND id NOT IN (
            SELECT id FROM password_history
            WHERE user_id = :userId
            ORDER BY created_at DESC
            LIMIT :keepCount
        )
        """, nativeQuery = true)
    void deleteOldEntries(@Param("userId") Long userId, @Param("keepCount") int keepCount);

    /**
     * Delete all password history for a user.
     */
    void deleteByUserId(Long userId);
}

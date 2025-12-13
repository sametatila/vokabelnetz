package com.vokabelnetz.repository;

import com.vokabelnetz.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Find users whose local midnight is at the given UTC hour.
     * Used for streak processing scheduler.
     */
    @Query(value = """
        SELECT * FROM users
        WHERE deleted_at IS NULL
        AND is_active = true
        AND EXTRACT(HOUR FROM NOW() AT TIME ZONE timezone) = 0
        """, nativeQuery = true)
    List<User> findUsersWithMidnightAt(@Param("utcHour") int utcHour);

    /**
     * Find users who need streak reminder.
     */
    @Query("""
        SELECT u FROM User u
        JOIN u.preferences p
        WHERE u.deletedAt IS NULL
        AND u.isActive = true
        AND p.streakReminders = true
        """)
    List<User> findUsersNeedingStreakReminder();

    /**
     * Find all active users (not deleted).
     */
    List<User> findByIsActiveTrueAndDeletedAtIsNull();

    /**
     * Permanently delete soft-deleted users older than cutoff date.
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :cutoffDate")
    int deleteByDeletedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}

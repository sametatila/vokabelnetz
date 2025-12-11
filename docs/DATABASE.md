# 💾 Database Schema

This document describes the PostgreSQL database schema, migrations, and data seeding strategy for Vokabelnetz.

## Table of Contents

- [Overview](#overview)
- [Entity Relationship Diagram](#entity-relationship-diagram)
- [Tables](#tables)
- [ENUM Types](#enum-types)
- [Indexes](#indexes)
- [Triggers](#triggers)
- [Data Seeding Strategy](#data-seeding-strategy)
- [Migrations](#migrations)

---

## Overview

| Property | Value |
|----------|-------|
| Database | PostgreSQL 18 |
| ORM | Hibernate 7.1 |
| Migration Tool | Flyway |
| Connection Pool | HikariCP |

---

## Entity Relationship Diagram

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                              DATABASE SCHEMA                                       │
├───────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│  ┌─────────────────────────┐              ┌─────────────────────────────┐         │
│  │         users           │              │          words              │         │
│  ├─────────────────────────┤              ├─────────────────────────────┤         │
│  │ id (PK) BIGSERIAL       │              │ id (PK) BIGSERIAL           │         │
│  │ email VARCHAR(255) NN   │              │ german VARCHAR(255) NN      │         │
│  │ password_hash VARCHAR   │              │ turkish VARCHAR(255)        │         │
│  │ display_name VARCHAR    │              │ english VARCHAR(255)        │         │
│  │ elo_rating INT [1000]   │              │ article VARCHAR(10)         │         │
│  │ current_streak INT [0]  │              │ word_type ENUM              │         │
│  │ longest_streak INT [0]  │              │ cefr_level ENUM NN          │         │
│  │ ui_language ENUM        │              │ example_sentence_de TEXT    │         │
│  │ source_language ENUM    │              │ example_sentence_tr TEXT    │         │
│  │ achievements JSONB []   │              │ example_sentence_en TEXT    │         │
│  │ role VARCHAR [USER]     │              │ difficulty_rating INT [1000]│         │
│  │ deleted_at TIMESTAMP    │              │ category ENUM               │         │
│  │ created_at TIMESTAMP    │              │ category ENUM               │         │
│  │ updated_at TIMESTAMP    │              │ created_at TIMESTAMP        │         │
│  └─────────────┬───────────┘              └──────────────┬──────────────┘         │
│                │                                         │                        │
│  UNIQUE INDEX: email WHERE deleted_at IS NULL            │                        │
│                │ 1:N                               1:N   │                        │
│                │                                         │                        │
│                ▼                                         ▼                        │
│  ┌─────────────────────────────────────────────────────────────────────┐          │
│  │                      user_word_progress                              │          │
│  ├─────────────────────────────────────────────────────────────────────┤          │
│  │ id (PK) BIGSERIAL                                                   │          │
│  │ user_id (FK) → users(id)                                            │          │
│  │ word_id (FK) → words(id)                                            │          │
│  │ ease_factor DECIMAL(4,2) [2.5]    -- SM-2 ease factor               │          │
│  │ repetition INT [0]                 -- SM-2 repetition count          │          │
│  │ interval_days INT [1]              -- SM-2 interval                  │          │
│  │ next_review_at TIMESTAMP                                             │          │
│  │ is_learned BOOLEAN [F]                                               │          │
│  │ UNIQUE(user_id, word_id)                                             │          │
│  └─────────────────────────────────────────────────────────────────────┘          │
│                                                                                    │
│  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────────┐          │
│  │ learning_sessions │  │  refresh_tokens   │  │   user_preferences    │          │
│  │                   │  │                   │  │                       │          │
│  │ session_type ENUM │  │ token TEXT UQ     │  │ ui_language ENUM      │          │
│  │ cefr_level ENUM   │  │ expires_at TS     │  │ source_language ENUM  │          │
│  │ words_reviewed    │  │ is_revoked BOOL   │  │ daily_word_goal INT   │          │
│  │ accuracy DECIMAL  │  │                   │  │ dark_mode BOOLEAN     │          │
│  └───────────────────┘  └───────────────────┘  └───────────────────────┘          │
│                                                                                    │
│  ┌───────────────────┐  ┌───────────────────┐                                     │
│  │    daily_stats    │  │  streak_history   │                                     │
│  │                   │  │                   │                                     │
│  │ stat_date DATE UQ │  │ streak_date DATE  │                                     │
│  │ words_learned     │  │ streak_count INT  │                                     │
│  │ words_reviewed    │  │ freeze_used BOOL  │                                     │
│  │ elo_start/end     │  │                   │                                     │
│  └───────────────────┘  └───────────────────┘                                     │
│                                                                                    │
│  Note: email uniqueness enforced via partial index (WHERE deleted_at IS NULL)     │
│  Note: achievements stored as JSONB array in users table                          │
│                                                                                    │
└───────────────────────────────────────────────────────────────────────────────────┘

Legend: PK = Primary Key, FK = Foreign Key, UQ = Unique, NN = Not Null, [x] = Default
```

---

## Tables

### users

Core user account information with soft delete support for GDPR compliance.

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,  -- UNIQUE removed, see partial index below
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    
    -- Elo Rating
    elo_rating INT DEFAULT 1000 
        CHECK (elo_rating >= 0 AND elo_rating <= 3000),
    
    -- Streaks
    current_streak INT DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak INT DEFAULT 0 CHECK (longest_streak >= 0),
    streak_freezes_available INT DEFAULT 0 
        CHECK (streak_freezes_available >= 0 AND streak_freezes_available <= 3),
    streak_freeze_used_at DATE,
    
    -- Stats
    total_words_learned INT DEFAULT 0 CHECK (total_words_learned >= 0),
    daily_goal INT DEFAULT 20 CHECK (daily_goal >= 1 AND daily_goal <= 200),
    
    -- Language (denormalized for quick access)
    -- Note: Actual default determined by frontend from system language
    -- Falls back to 'en' if system language is not TR or EN
    ui_language ui_language_code DEFAULT 'en',
    source_language source_language_code DEFAULT 'en',
    
    -- Timezone (for streak calculations)
    -- Default: Europe/Istanbul (UTC+2/+3) if system timezone unavailable
    timezone VARCHAR(50) DEFAULT 'Europe/Istanbul',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    
    -- Soft Delete (GDPR compliance)
    -- NULL = active user, NOT NULL = deletion requested
    -- Hard delete via cron job after 30 days
    deleted_at TIMESTAMP DEFAULT NULL,
    deletion_reason VARCHAR(255),
    
    -- Achievements (stored as JSONB array)
    -- Example: ["FIRST_WORD", "7_DAY_STREAK", "100_WORDS"]
    achievements JSONB DEFAULT '[]',
    
    -- Role-based access control
    role VARCHAR(20) DEFAULT 'ROLE_USER' 
        CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPER')),
    
    -- Password tracking
    password_changed_at TIMESTAMP DEFAULT NOW(),
    
    -- Timestamps
    last_login_at TIMESTAMP,
    last_active_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- CRITICAL: Partial unique index for email
-- Allows same email to exist in soft-deleted rows while preventing
-- duplicate emails among active users
CREATE UNIQUE INDEX idx_users_email_unique ON users(email) WHERE deleted_at IS NULL;

-- Partial index for active users only (most queries)
CREATE INDEX idx_users_active ON users(id) WHERE deleted_at IS NULL;
```

> **Soft Delete Strategy:** When a user requests account deletion, `deleted_at` is set to current timestamp. A scheduled job permanently deletes records after 30 days, satisfying GDPR's "right to be forgotten" while allowing recovery during grace period.

> **Email Uniqueness:** The partial unique index `idx_users_email_unique` ensures email uniqueness only among active users (`deleted_at IS NULL`). This allows a user to re-register with the same email after their previous account was soft-deleted.
```

### words

German vocabulary from Goethe Institut word lists.

```sql
CREATE TABLE words (
    id BIGSERIAL PRIMARY KEY,
    
    -- German word (target language)
    german VARCHAR(255) NOT NULL,
    article VARCHAR(10),              -- der, die, das
    plural_form VARCHAR(255),
    
    -- Translations
    turkish VARCHAR(255),
    english VARCHAR(255),
    
    -- Grammar
    word_type word_type DEFAULT 'OTHER',
    
    -- CEFR Level
    cefr_level cefr_level NOT NULL,
    
    -- Example sentences (trilingual)
    example_sentence_de TEXT,
    example_sentence_tr TEXT,
    example_sentence_en TEXT,
    
    -- Pronunciation
    pronunciation_url VARCHAR(500),
    phonetic_spelling VARCHAR(255),   -- IPA notation
    
    -- Difficulty & Classification
    difficulty_rating INT DEFAULT 1000 
        CHECK (difficulty_rating >= 0 AND difficulty_rating <= 3000),
    category word_category DEFAULT 'ANDERE',
    tags VARCHAR(500),                -- Comma-separated
    
    -- Global Statistics
    times_shown INT DEFAULT 0,
    times_correct INT DEFAULT 0,
    
    -- Metadata
    source VARCHAR(100) DEFAULT 'goethe',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- CRITICAL: Homonym-safe unique constraint
-- Includes article and word_type to distinguish:
--   "das Band" (ribbon) vs "die Band" (music band)
--   "der Leiter" (leader) vs "die Leiter" (ladder)
-- COALESCE handles NULL articles (for verbs, adjectives, etc.)
CREATE UNIQUE INDEX idx_words_unique_entry 
    ON words(german, cefr_level, COALESCE(article, ''), word_type);
```

> **Homonym Handling:** German has many homonyms - words spelled the same but with different meanings, genders, or types. The unique index includes `article` and `word_type` to prevent data loss during upserts. For example, "das Band" (A2, ribbon) and "die Band" (A2, music band) are stored as separate entries.

### user_word_progress

Tracks individual user progress on each word (SM-2 algorithm data).

```sql
CREATE TABLE user_word_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    
    -- SM-2 Algorithm Fields
    ease_factor DECIMAL(4,2) DEFAULT 2.5 
        CHECK (ease_factor >= 1.3 AND ease_factor <= 5.0),
    repetition INT DEFAULT 0 CHECK (repetition >= 0),
    interval_days INT DEFAULT 1 CHECK (interval_days >= 0),
    next_review_at TIMESTAMP,
    
    -- Answer Statistics
    times_correct INT DEFAULT 0 CHECK (times_correct >= 0),
    times_incorrect INT DEFAULT 0 CHECK (times_incorrect >= 0),
    last_quality INT CHECK (last_quality >= 0 AND last_quality <= 5),
    last_response_time_ms INT,
    
    -- Learning Status
    is_learned BOOLEAN DEFAULT FALSE,
    learned_at TIMESTAMP,
    first_seen_at TIMESTAMP DEFAULT NOW(),
    last_reviewed_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(user_id, word_id)
);
```

### learning_sessions

Records individual learning sessions.

```sql
CREATE TABLE learning_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Session Configuration
    session_type session_type DEFAULT 'MIXED',
    cefr_level cefr_level,
    
    -- Timing
    started_at TIMESTAMP DEFAULT NOW(),
    ended_at TIMESTAMP,
    duration_seconds INT,
    
    -- Results
    words_reviewed INT DEFAULT 0,
    words_new INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    incorrect_answers INT DEFAULT 0,
    accuracy DECIMAL(5,2),
    
    -- Rating Change
    elo_change INT,
    
    -- Status
    is_completed BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT NOW()
);
```

### user_preferences

User settings and preferences.

```sql
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Language Settings
    -- Note: Defaults here are fallbacks. Frontend detects system language first.
    -- If system language is TR → use 'tr', otherwise → use 'en'
    ui_language ui_language_code DEFAULT 'en',
    source_language source_language_code DEFAULT 'en',
    target_language target_language_code DEFAULT 'de',
    show_both_translations BOOLEAN DEFAULT FALSE,
    primary_translation source_language_code DEFAULT 'tr',
    
    -- Learning Goals
    daily_word_goal INT DEFAULT 20 
        CHECK (daily_word_goal >= 1 AND daily_word_goal <= 200),
    session_duration_min INT DEFAULT 15 
        CHECK (session_duration_min >= 5 AND session_duration_min <= 120),
    new_words_per_session INT DEFAULT 5 
        CHECK (new_words_per_session >= 1 AND new_words_per_session <= 50),
    
    -- Notifications
    notification_enabled BOOLEAN DEFAULT TRUE,
    notification_time TIME DEFAULT '09:00:00',
    email_reminders BOOLEAN DEFAULT FALSE,
    
    -- UI Preferences
    sound_enabled BOOLEAN DEFAULT TRUE,
    dark_mode BOOLEAN DEFAULT FALSE,
    show_pronunciation BOOLEAN DEFAULT TRUE,
    auto_play_audio BOOLEAN DEFAULT FALSE,
    show_example_sentences BOOLEAN DEFAULT TRUE,
    show_word_type BOOLEAN DEFAULT TRUE,
    show_article_hints BOOLEAN DEFAULT TRUE,
    compact_card_view BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### daily_stats

Daily learning statistics for progress tracking.

```sql
CREATE TABLE daily_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,
    
    -- Learning Metrics
    words_learned INT DEFAULT 0 CHECK (words_learned >= 0),
    words_reviewed INT DEFAULT 0 CHECK (words_reviewed >= 0),
    correct_answers INT DEFAULT 0 CHECK (correct_answers >= 0),
    incorrect_answers INT DEFAULT 0 CHECK (incorrect_answers >= 0),
    
    -- Time & Sessions
    total_time_seconds INT DEFAULT 0 CHECK (total_time_seconds >= 0),
    sessions_count INT DEFAULT 0 CHECK (sessions_count >= 0),
    
    -- Streak & Rating
    streak_maintained BOOLEAN DEFAULT FALSE,
    elo_start INT,
    elo_end INT,
    
    created_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(user_id, stat_date)
);
```

### streak_history

Detailed streak tracking for analytics.

```sql
CREATE TABLE streak_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    streak_date DATE NOT NULL,
    streak_count INT NOT NULL,
    words_completed INT DEFAULT 0,
    goal_met BOOLEAN DEFAULT FALSE,
    freeze_used BOOLEAN DEFAULT FALSE,
    streak_broken BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(user_id, streak_date)
);
```

### refresh_tokens

JWT refresh token storage.

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- TEXT instead of VARCHAR to avoid length issues with JWT tokens
    -- Modern JWTs with HS512 signatures can exceed 500 characters
    token TEXT UNIQUE NOT NULL,
    
    device_info VARCHAR(255),
    ip_address VARCHAR(50),
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(50),  -- LOGOUT, ROTATION, PASSWORD_CHANGE, USER_REVOKED, etc.
    created_at TIMESTAMP DEFAULT NOW()
);
```

> **Token Storage:** Using `TEXT` instead of `VARCHAR(500)` for JWT storage. Modern JWTs with claims and HS512 signatures can exceed 500 characters. PostgreSQL `TEXT` has no length limit and identical performance to `VARCHAR`.

### password_reset_tokens

Secure password reset token storage. Tokens are hashed before storage.

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Token: 32 random bytes, hex encoded (64 chars)
    -- Stored as SHA-256 hash to prevent DB leak exposure
    token_hash VARCHAR(64) NOT NULL,
    
    -- Short expiration: 1 hour
    expires_at TIMESTAMP NOT NULL,
    
    -- Single use enforcement
    used_at TIMESTAMP,
    
    -- Audit info
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    
    created_at TIMESTAMP DEFAULT NOW()
);

-- Index for token lookup
CREATE INDEX idx_reset_tokens_hash ON password_reset_tokens(token_hash);

-- Index for cleanup job
CREATE INDEX idx_reset_tokens_expires ON password_reset_tokens(expires_at);
```

> **Token Security:** Plain tokens are NEVER stored. Only SHA-256 hash is saved. Tokens are single-use (marked via `used_at`) and expire in 1 hour.

### password_history

Prevents password reuse by storing hashed previous passwords.

```sql
CREATE TABLE password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_password_history_user ON password_history(user_id);
```

> **Password Reuse Prevention:** Last 5 password hashes are stored. New passwords are checked against history before acceptance.

---

## ENUM Types

```sql
-- CEFR proficiency levels
CREATE TYPE cefr_level AS ENUM ('A1', 'A2', 'B1', 'B2', 'C1', 'C2');

-- Word grammatical types
CREATE TYPE word_type AS ENUM (
    'NOUN', 'VERB', 'ADJECTIVE', 'ADVERB', 
    'PREPOSITION', 'CONJUNCTION', 'PRONOUN', 'ARTICLE', 'OTHER'
);

-- Vocabulary categories
CREATE TYPE word_category AS ENUM (
    'ALLTAG',           -- Daily life
    'ARBEIT_BERUF',     -- Work & career
    'BILDUNG',          -- Education
    'ESSEN_TRINKEN',    -- Food & drink
    'FAMILIE_FREUNDE',  -- Family & friends
    'FREIZEIT',         -- Leisure
    'GESUNDHEIT',       -- Health
    'REISEN_VERKEHR',   -- Travel & transport
    'WOHNEN',           -- Home & living
    'ANDERE'            -- Other
);

-- Learning session types
CREATE TYPE session_type AS ENUM ('LEARN', 'REVIEW', 'QUIZ', 'MIXED');

-- Language codes
CREATE TYPE ui_language_code AS ENUM ('tr', 'en', 'de');
CREATE TYPE source_language_code AS ENUM ('tr', 'en');
CREATE TYPE target_language_code AS ENUM ('de');
```

---

## Achievements System

Achievements are stored as a JSONB array in the `users` table for simplicity. This avoids extra tables while allowing flexible achievement definitions.

### Achievement Definitions (Application Code)

```java
// Achievement definitions are hardcoded in application
public enum AchievementType {
    // Word Milestones
    FIRST_WORD("First Word", "Learn your first word", "🎉"),
    WORDS_10("Getting Started", "Learn 10 words", "📚"),
    WORDS_50("Word Collector", "Learn 50 words", "📖"),
    WORDS_100("Century", "Learn 100 words", "💯"),
    WORDS_250("Vocabulary Builder", "Learn 250 words", "🏗️"),
    WORDS_500("Word Master", "Learn 500 words", "🎓"),
    WORDS_1000("Lexicon Expert", "Learn 1000 words", "👑"),
    
    // Streak Milestones
    STREAK_3("Three's a Charm", "Maintain a 3-day streak", "🔥"),
    STREAK_7("Week Warrior", "Maintain a 7-day streak", "📅"),
    STREAK_14("Fortnight Fighter", "Maintain a 14-day streak", "⚔️"),
    STREAK_30("Monthly Master", "Maintain a 30-day streak", "🏆"),
    STREAK_100("Century Streak", "Maintain a 100-day streak", "💎"),
    
    // Accuracy Achievements
    PERFECT_SESSION("Perfect Session", "100% accuracy in a session", "✨"),
    ACCURACY_90("Sharp Mind", "Maintain 90%+ overall accuracy", "🎯"),
    
    // Level Achievements
    COMPLETE_A1("A1 Complete", "Learn all A1 words", "🥉"),
    COMPLETE_A2("A2 Complete", "Learn all A2 words", "🥈"),
    COMPLETE_B1("B1 Complete", "Learn all B1 words", "🥇"),
    
    // Special
    EARLY_BIRD("Early Bird", "Study before 7 AM", "🌅"),
    NIGHT_OWL("Night Owl", "Study after 11 PM", "🦉"),
    COMEBACK("Welcome Back", "Return after 30+ days", "👋");
    
    private final String name;
    private final String description;
    private final String icon;
}
```

### JSONB Storage Format

```json
// users.achievements column example
[
    {
        "type": "FIRST_WORD",
        "earnedAt": "2025-01-01T10:30:00Z"
    },
    {
        "type": "STREAK_7",
        "earnedAt": "2025-01-08T23:59:00Z"
    },
    {
        "type": "WORDS_100",
        "earnedAt": "2025-01-15T14:20:00Z"
    }
]
```

### Query Examples

```sql
-- Get users with specific achievement
SELECT * FROM users 
WHERE achievements @> '[{"type": "STREAK_30"}]';

-- Count users with achievement
SELECT COUNT(*) FROM users 
WHERE achievements @> '[{"type": "WORDS_100"}]';

-- Get all achievements for a user (in application)
SELECT achievements FROM users WHERE id = 1;
```

### AchievementService Implementation

```java
@Service
public class AchievementService {
    
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Check and award achievements after an action.
     */
    @Transactional
    public List<Achievement> checkAchievements(User user, AchievementContext context) {
        List<Achievement> newAchievements = new ArrayList<>();
        Set<String> existingTypes = getExistingAchievementTypes(user);
        
        // Check word milestones
        checkWordMilestones(user, existingTypes, newAchievements);
        
        // Check streak milestones
        checkStreakMilestones(user, existingTypes, newAchievements);
        
        // Check session-specific achievements
        if (context.getSessionAccuracy() != null && context.getSessionAccuracy() == 100.0) {
            awardIfNew(AchievementType.PERFECT_SESSION, existingTypes, newAchievements);
        }
        
        // Save new achievements
        if (!newAchievements.isEmpty()) {
            addAchievementsToUser(user, newAchievements);
        }
        
        return newAchievements;
    }
    
    private void checkWordMilestones(User user, Set<String> existing, List<Achievement> newList) {
        int wordsLearned = user.getTotalWordsLearned();
        
        Map<Integer, AchievementType> milestones = Map.of(
            1, AchievementType.FIRST_WORD,
            10, AchievementType.WORDS_10,
            50, AchievementType.WORDS_50,
            100, AchievementType.WORDS_100,
            250, AchievementType.WORDS_250,
            500, AchievementType.WORDS_500,
            1000, AchievementType.WORDS_1000
        );
        
        milestones.forEach((threshold, type) -> {
            if (wordsLearned >= threshold) {
                awardIfNew(type, existing, newList);
            }
        });
    }
    
    private void checkStreakMilestones(User user, Set<String> existing, List<Achievement> newList) {
        int streak = user.getCurrentStreak();
        
        Map<Integer, AchievementType> milestones = Map.of(
            3, AchievementType.STREAK_3,
            7, AchievementType.STREAK_7,
            14, AchievementType.STREAK_14,
            30, AchievementType.STREAK_30,
            100, AchievementType.STREAK_100
        );
        
        milestones.forEach((threshold, type) -> {
            if (streak >= threshold) {
                awardIfNew(type, existing, newList);
            }
        });
    }
    
    private void awardIfNew(AchievementType type, Set<String> existing, List<Achievement> newList) {
        if (!existing.contains(type.name())) {
            newList.add(new Achievement(type, LocalDateTime.now()));
        }
    }
    
    private void addAchievementsToUser(User user, List<Achievement> achievements) {
        try {
            List<Map<String, Object>> current = objectMapper.readValue(
                user.getAchievements(), 
                new TypeReference<>() {}
            );
            
            for (Achievement a : achievements) {
                current.add(Map.of(
                    "type", a.getType().name(),
                    "earnedAt", a.getEarnedAt().toString()
                ));
            }
            
            user.setAchievements(objectMapper.writeValueAsString(current));
            userRepository.save(user);
        } catch (JsonProcessingException e) {
            log.error("Failed to update achievements for user {}", user.getId(), e);
        }
    }
}
```

### Extension: pg_trgm (Fuzzy Search)

Enable trigram extension for fuzzy text search (handles typos like "Hau" → "Haus"):

```sql
-- Enable trigram extension (run once)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

### Standard Indexes

```sql
-- Users
-- CRITICAL: Partial unique index for email (allows same email in soft-deleted rows)
CREATE UNIQUE INDEX idx_users_email_unique ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email ON users(email);  -- For lookups including deleted
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_elo_rating ON users(elo_rating);
CREATE INDEX idx_users_last_active ON users(last_active_at);
CREATE INDEX idx_users_active ON users(id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_pending_deletion ON users(deleted_at) WHERE deleted_at IS NOT NULL;

-- Words
-- CRITICAL: Homonym-safe unique index (defined in table creation, listed here for reference)
-- CREATE UNIQUE INDEX idx_words_unique_entry ON words(german, cefr_level, COALESCE(article, ''), word_type);
CREATE INDEX idx_words_cefr_level ON words(cefr_level);
CREATE INDEX idx_words_category ON words(category);
CREATE INDEX idx_words_difficulty ON words(difficulty_rating);
CREATE INDEX idx_words_german ON words(german);
CREATE INDEX idx_words_cefr_difficulty ON words(cefr_level, difficulty_rating);

-- User Word Progress (critical for learning queries)
CREATE INDEX idx_progress_user_id ON user_word_progress(user_id);
CREATE INDEX idx_progress_word_id ON user_word_progress(word_id);
CREATE INDEX idx_progress_next_review ON user_word_progress(next_review_at);
CREATE INDEX idx_progress_user_next_review ON user_word_progress(user_id, next_review_at);
CREATE INDEX idx_progress_is_learned ON user_word_progress(is_learned);
CREATE INDEX idx_progress_user_learned ON user_word_progress(user_id, is_learned);

-- Learning Sessions
CREATE INDEX idx_sessions_user_id ON learning_sessions(user_id);
CREATE INDEX idx_sessions_started_at ON learning_sessions(started_at);
CREATE INDEX idx_sessions_user_started ON learning_sessions(user_id, started_at DESC);

-- Daily Stats
CREATE INDEX idx_daily_stats_user_id ON daily_stats(user_id);
CREATE INDEX idx_daily_stats_date ON daily_stats(stat_date);
CREATE INDEX idx_daily_stats_user_date ON daily_stats(user_id, stat_date DESC);

-- Streak History
CREATE INDEX idx_streak_history_user_date ON streak_history(user_id, streak_date DESC);

-- Refresh Tokens
CREATE INDEX idx_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_tokens_is_revoked ON refresh_tokens(is_revoked);
```

### Trigram Indexes (Fuzzy Search)

```sql
-- GIN indexes for fuzzy text search using pg_trgm
-- Enables queries like: WHERE german % 'Hau' (finds "Haus")
-- Enables queries like: WHERE german ILIKE '%arbe%' (fast pattern matching)

CREATE INDEX idx_words_german_trgm ON words USING GIN (german gin_trgm_ops);
CREATE INDEX idx_words_turkish_trgm ON words USING GIN (turkish gin_trgm_ops);
CREATE INDEX idx_words_english_trgm ON words USING GIN (english gin_trgm_ops);
```

### Fuzzy Search Query Examples

```sql
-- Find words similar to "Hau" (handles typos)
SELECT * FROM words 
WHERE german % 'Hau' 
ORDER BY similarity(german, 'Hau') DESC 
LIMIT 10;

-- Find words containing pattern (fast with GIN index)
SELECT * FROM words 
WHERE german ILIKE '%arbeit%';

-- Combined: fuzzy match with similarity threshold
SELECT *, similarity(german, 'arbeiten') AS sim 
FROM words 
WHERE german % 'arbeiten' AND similarity(german, 'arbeiten') > 0.3
ORDER BY sim DESC;
```

---

## Triggers

### Auto-Update Timestamps

```sql
-- Function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply to tables
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_words_updated_at
    BEFORE UPDATE ON words
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_word_progress_updated_at
    BEFORE UPDATE ON user_word_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

---

## Data Seeding Strategy

### Pipeline Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DATA SEEDING PIPELINE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐  │
│  │  JSON Source    │    │   Validation    │    │    Database Insert      │  │
│  │  Files          │───▶│   & Transform   │───▶│    (Batch Processing)   │  │
│  │                 │    │                 │    │                         │  │
│  │  words-a1.json  │    │  - Schema check │    │  - Upsert logic         │  │
│  │  words-a2.json  │    │  - Type mapping │    │  - Conflict handling    │  │
│  │  words-b1.json  │    │  - Enrichment   │    │  - Transaction mgmt     │  │
│  └─────────────────┘    └─────────────────┘    └─────────────────────────┘  │
│                                                                              │
│  Execution Modes:                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  1. INIT     - First-time full load (truncate + insert)              │   │
│  │  2. UPDATE   - Incremental updates (upsert, preserve user progress)  │   │
│  │  3. VALIDATE - Dry-run to check data integrity                       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### JSON Source Format

**data/words-a1.json**
```json
{
  "metadata": {
    "level": "A1",
    "source": "Goethe Institut Wortliste",
    "version": "2024.1",
    "wordCount": 650,
    "lastUpdated": "2024-12-01"
  },
  "words": [
    {
      "german": "arbeiten",
      "article": null,
      "wordType": "VERB",
      "plural": null,
      "translations": {
        "turkish": "çalışmak",
        "english": "to work"
      },
      "examples": {
        "german": "Ich arbeite in einem Büro.",
        "turkish": "Bir ofiste çalışıyorum.",
        "english": "I work in an office."
      },
      "category": "ARBEIT_BERUF",
      "tags": ["daily", "common"],
      "pronunciation": {
        "ipa": "ˈaʁbaɪ̯tn̩",
        "audioFile": "arbeiten.mp3"
      },
      "difficulty": {
        "initial": 900,
        "notes": "Common verb, regular conjugation"
      }
    },
    {
      "german": "Haus",
      "article": "das",
      "wordType": "NOUN",
      "plural": "Häuser",
      "translations": {
        "turkish": "ev",
        "english": "house"
      },
      "examples": {
        "german": "Wir wohnen in einem großen Haus.",
        "turkish": "Büyük bir evde yaşıyoruz.",
        "english": "We live in a big house."
      },
      "category": "WOHNEN",
      "tags": ["home", "building"],
      "pronunciation": {
        "ipa": "haʊ̯s",
        "audioFile": "haus.mp3"
      },
      "difficulty": {
        "initial": 800,
        "notes": "Very common noun, neuter"
      }
    }
  ]
}
```

### DataInitializer Implementation

```java
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {
    
    private final WordRepository wordRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.data.seed-mode:VALIDATE}")
    private SeedMode seedMode;
    
    @Value("${app.data.path:classpath:data/}")
    private String dataPath;
    
    private static final int BATCH_SIZE = 500;
    
    public enum SeedMode {
        INIT,      // Truncate and insert all
        UPDATE,    // Upsert (preserve user progress)
        VALIDATE   // Dry-run only
    }
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization in {} mode", seedMode);
        
        List<String> files = List.of(
            "words-a1.json",
            "words-a2.json", 
            "words-b1.json"
        );
        
        int totalProcessed = 0;
        int totalErrors = 0;
        
        for (String file : files) {
            Result result = processFile(file);
            totalProcessed += result.processed();
            totalErrors += result.errors();
        }
        
        log.info("Data initialization complete. Processed: {}, Errors: {}", 
            totalProcessed, totalErrors);
    }
    
    private Result processFile(String filename) throws IOException {
        Resource resource = resourceLoader.getResource(dataPath + filename);
        WordFileDTO fileData = objectMapper.readValue(
            resource.getInputStream(), 
            WordFileDTO.class
        );
        
        log.info("Processing {} - {} words (version {})", 
            filename, 
            fileData.getMetadata().getWordCount(),
            fileData.getMetadata().getVersion()
        );
        
        List<Word> words = fileData.getWords().stream()
            .map(this::mapToEntity)
            .filter(Objects::nonNull)
            .toList();
        
        if (seedMode == SeedMode.VALIDATE) {
            log.info("VALIDATE mode - {} words parsed successfully", words.size());
            return new Result(words.size(), 0);
        }
        
        return saveBatch(words);
    }
    
    private Result saveBatch(List<Word> words) {
        int processed = 0;
        int errors = 0;
        
        for (int i = 0; i < words.size(); i += BATCH_SIZE) {
            List<Word> batch = words.subList(
                i, 
                Math.min(i + BATCH_SIZE, words.size())
            );
            
            try {
                if (seedMode == SeedMode.INIT) {
                    wordRepository.saveAll(batch);
                } else {
                    // UPDATE mode - upsert
                    for (Word word : batch) {
                        wordRepository.upsert(word);
                    }
                }
                processed += batch.size();
            } catch (Exception e) {
                log.error("Error saving batch starting at index {}: {}", 
                    i, e.getMessage());
                errors += batch.size();
            }
        }
        
        return new Result(processed, errors);
    }
    
    private Word mapToEntity(WordDTO dto) {
        try {
            return Word.builder()
                .german(dto.getGerman())
                .turkish(dto.getTranslations().getTurkish())
                .english(dto.getTranslations().getEnglish())
                .article(dto.getArticle())
                .wordType(WordType.valueOf(dto.getWordType()))
                .cefrLevel(CefrLevel.valueOf(dto.getLevel()))
                .pluralForm(dto.getPlural())
                .exampleSentenceDe(dto.getExamples().getGerman())
                .exampleSentenceTr(dto.getExamples().getTurkish())
                .exampleSentenceEn(dto.getExamples().getEnglish())
                .category(WordCategory.valueOf(dto.getCategory()))
                .phoneticSpelling(dto.getPronunciation().getIpa())
                .pronunciationUrl("/assets/audio/" + dto.getPronunciation().getAudioFile())
                .difficultyRating(dto.getDifficulty().getInitial())
                .tags(String.join(",", dto.getTags()))
                .build();
        } catch (Exception e) {
            log.warn("Failed to map word '{}': {}", dto.getGerman(), e.getMessage());
            return null;
        }
    }
    
    private record Result(int processed, int errors) {}
}
```

### Upsert Query

```java
@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    
    @Modifying
    @Query(value = """
        INSERT INTO words (german, turkish, english, article, word_type, 
                          cefr_level, example_sentence_de, example_sentence_tr,
                          example_sentence_en, category, difficulty_rating, 
                          pronunciation_url, phonetic_spelling, plural_form, 
                          tags, created_at, updated_at)
        VALUES (:#{#word.german}, :#{#word.turkish}, :#{#word.english}, 
                :#{#word.article}, :#{#word.wordType?.name()}, 
                :#{#word.cefrLevel?.name()}, :#{#word.exampleSentenceDe},
                :#{#word.exampleSentenceTr}, :#{#word.exampleSentenceEn},
                :#{#word.category?.name()}, :#{#word.difficultyRating},
                :#{#word.pronunciationUrl}, :#{#word.phoneticSpelling},
                :#{#word.pluralForm}, :#{#word.tags}, NOW(), NOW())
        ON CONFLICT (german, cefr_level, COALESCE(article, ''), word_type) DO UPDATE SET
            turkish = EXCLUDED.turkish,
            english = EXCLUDED.english,
            example_sentence_de = EXCLUDED.example_sentence_de,
            example_sentence_tr = EXCLUDED.example_sentence_tr,
            example_sentence_en = EXCLUDED.example_sentence_en,
            pronunciation_url = EXCLUDED.pronunciation_url,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("word") Word word);
}
```

> **Homonym-Safe Upsert:** The `ON CONFLICT` clause now includes `article` and `word_type` to prevent homonyms from overwriting each other. For example:
> - "das Band" (ribbon, NOUN) and "die Band" (music band, NOUN) → Two separate entries
> - "laufen" (to run, VERB) won't conflict with "das Laufen" (running, NOUN)

---

## Migrations

### Flyway Structure

```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_language_settings.sql
├── V3__add_streak_system.sql
├── V4__add_daily_stats.sql
├── V5__add_audio_fields.sql
├── V6__add_soft_delete.sql
├── V7__add_fuzzy_search.sql
├── V8__add_achievements.sql
├── V9__add_security_tables.sql
└── V10__add_roles.sql
```

### Example Migration: V3__add_streak_system.sql

```sql
-- Add streak freeze columns to users
ALTER TABLE users 
    ADD COLUMN streak_freezes_available INT DEFAULT 0 
        CHECK (streak_freezes_available >= 0 AND streak_freezes_available <= 3),
    ADD COLUMN streak_freeze_used_at DATE,
    ADD COLUMN timezone VARCHAR(50) DEFAULT 'Europe/Istanbul';

-- Create streak history table
CREATE TABLE streak_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    streak_date DATE NOT NULL,
    streak_count INT NOT NULL,
    words_completed INT DEFAULT 0,
    goal_met BOOLEAN DEFAULT FALSE,
    freeze_used BOOLEAN DEFAULT FALSE,
    streak_broken BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, streak_date)
);

CREATE INDEX idx_streak_history_user_date 
    ON streak_history(user_id, streak_date DESC);
```

### Example Migration: V6__add_soft_delete.sql

```sql
-- Add soft delete support for GDPR compliance
-- Users are marked for deletion, then permanently removed after 30 days

ALTER TABLE users 
    ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL,
    ADD COLUMN deletion_reason VARCHAR(255);

-- Partial index for active users (most common query pattern)
CREATE INDEX idx_users_active ON users(id) WHERE deleted_at IS NULL;

-- Index for cleanup job to find users pending hard delete
CREATE INDEX idx_users_pending_deletion ON users(deleted_at) 
    WHERE deleted_at IS NOT NULL;

COMMENT ON COLUMN users.deleted_at IS 
    'Soft delete timestamp. NULL = active, NOT NULL = pending deletion. Hard delete after 30 days.';
```

### Example Migration: V7__add_fuzzy_search.sql

```sql
-- Enable pg_trgm extension for fuzzy text search
-- Allows queries like: WHERE german % 'Hau' to find 'Haus'

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN indexes for fast trigram-based fuzzy search
CREATE INDEX idx_words_german_trgm ON words USING GIN (german gin_trgm_ops);
CREATE INDEX idx_words_turkish_trgm ON words USING GIN (turkish gin_trgm_ops);
CREATE INDEX idx_words_english_trgm ON words USING GIN (english gin_trgm_ops);

-- Set default similarity threshold (optional)
-- Can be adjusted per-query: SET pg_trgm.similarity_threshold = 0.3;
```

### Example Migration: V8__add_achievements.sql

```sql
-- Add achievements JSONB column to users table
-- Stores earned achievements as an array of objects

ALTER TABLE users 
    ADD COLUMN achievements JSONB DEFAULT '[]';

-- Index for querying users with specific achievements
CREATE INDEX idx_users_achievements ON users USING GIN (achievements);

-- Example: Find users with STREAK_30 achievement
-- SELECT * FROM users WHERE achievements @> '[{"type": "STREAK_30"}]';

COMMENT ON COLUMN users.achievements IS 
    'JSONB array of earned achievements. Format: [{"type": "ACHIEVEMENT_TYPE", "earnedAt": "ISO_TIMESTAMP"}]';
```

### Example Migration: V9__add_security_tables.sql

```sql
-- Password reset tokens (hashed, single-use, short-lived)
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,  -- SHA-256 hash of actual token
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_reset_tokens_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_reset_tokens_expires ON password_reset_tokens(expires_at);

-- Password history (prevents reuse)
CREATE TABLE password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_password_history_user ON password_history(user_id);

-- Add revocation tracking to refresh_tokens
ALTER TABLE refresh_tokens 
    ADD COLUMN revoked_at TIMESTAMP,
    ADD COLUMN revoked_reason VARCHAR(50);

-- Add password change tracking to users
ALTER TABLE users 
    ADD COLUMN password_changed_at TIMESTAMP DEFAULT NOW();

COMMENT ON TABLE password_reset_tokens IS 
    'Stores hashed password reset tokens. Plain tokens are NEVER stored.';
COMMENT ON TABLE password_history IS 
    'Last 5 passwords stored to prevent reuse.';
```

### Example Migration: V10__add_roles.sql

```sql
-- Add role-based access control
ALTER TABLE users 
    ADD COLUMN role VARCHAR(20) DEFAULT 'ROLE_USER'
    CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPER'));

CREATE INDEX idx_users_role ON users(role);

COMMENT ON COLUMN users.role IS 
    'User role: ROLE_USER (default), ROLE_ADMIN (content management), ROLE_SUPER (system admin)';
```

---

## Sample Data

```sql
-- Sample A1 vocabulary
INSERT INTO words (german, turkish, english, article, word_type, cefr_level, 
                   example_sentence_de, example_sentence_tr, example_sentence_en,
                   category, difficulty_rating)
VALUES 
('arbeiten', 'çalışmak', 'to work', NULL, 'VERB', 'A1', 
 'Ich arbeite in einem Büro.', 'Bir ofiste çalışıyorum.', 'I work in an office.',
 'ARBEIT_BERUF', 900),

('Haus', 'ev', 'house', 'das', 'NOUN', 'A1',
 'Wir wohnen in einem großen Haus.', 'Büyük bir evde yaşıyoruz.', 'We live in a big house.',
 'WOHNEN', 800),

('Familie', 'aile', 'family', 'die', 'NOUN', 'A1',
 'Meine Familie ist sehr groß.', 'Ailem çok büyük.', 'My family is very big.',
 'FAMILIE_FREUNDE', 850),

('essen', 'yemek', 'to eat', NULL, 'VERB', 'A1',
 'Wir essen zusammen zu Abend.', 'Birlikte akşam yemeği yiyoruz.', 'We eat dinner together.',
 'ESSEN_TRINKEN', 800),

('Freund', 'arkadaş', 'friend', 'der', 'NOUN', 'A1',
 'Er ist mein bester Freund.', 'O benim en iyi arkadaşım.', 'He is my best friend.',
 'FAMILIE_FREUNDE', 850);
```

-- =============================================
-- Vokabelnetz Database Schema
-- Version: 1.0.0
-- Based on DATABASE.md documentation
-- =============================================

-- =============================================
-- ENUM TYPES
-- =============================================

CREATE TYPE cefr_level AS ENUM ('A1', 'A2', 'B1', 'B2', 'C1', 'C2');
CREATE TYPE word_type AS ENUM ('NOUN', 'VERB', 'ADJECTIVE', 'ADVERB', 'PREPOSITION', 'CONJUNCTION', 'PRONOUN', 'ARTICLE', 'OTHER');
CREATE TYPE word_category AS ENUM ('ALLTAG', 'ARBEIT_BERUF', 'BILDUNG', 'ESSEN_TRINKEN', 'FAMILIE_FREUNDE', 'FREIZEIT', 'GESUNDHEIT', 'REISEN_VERKEHR', 'WOHNEN', 'ANDERE');
CREATE TYPE session_type AS ENUM ('LEARN', 'REVIEW', 'QUIZ', 'MIXED');
CREATE TYPE ui_language AS ENUM ('TR', 'EN', 'DE');
CREATE TYPE source_language AS ENUM ('TR', 'EN');
CREATE TYPE user_role AS ENUM ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPER');

-- =============================================
-- USERS TABLE
-- =============================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),

    -- Elo Rating
    elo_rating INTEGER DEFAULT 1000,

    -- Streaks
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    streak_freezes_available INTEGER DEFAULT 0,
    streak_freeze_used_at DATE,

    -- Stats
    total_words_learned INTEGER DEFAULT 0,
    daily_goal INTEGER DEFAULT 20,

    -- Language Settings
    ui_language VARCHAR(2) DEFAULT 'EN',
    source_language VARCHAR(2) DEFAULT 'EN',
    timezone VARCHAR(50) DEFAULT 'Europe/Istanbul',

    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,

    -- Soft Delete (GDPR)
    deleted_at TIMESTAMP,
    deletion_reason TEXT,

    -- Achievements (JSONB)
    achievements JSONB DEFAULT '[]',

    -- Role
    role VARCHAR(20) DEFAULT 'ROLE_USER',

    -- Password tracking
    password_changed_at TIMESTAMP,

    -- Activity tracking
    last_login_at TIMESTAMP,
    last_active_at TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for users
CREATE UNIQUE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);

-- =============================================
-- WORDS TABLE
-- =============================================

CREATE TABLE words (
    id BIGSERIAL PRIMARY KEY,
    german VARCHAR(200) NOT NULL,
    article VARCHAR(10),
    plural VARCHAR(200),

    -- Translations (JSONB)
    translations JSONB,

    -- Classification
    word_type VARCHAR(20),
    cefr_level VARCHAR(2) NOT NULL,
    category VARCHAR(30),

    -- Content
    example_sentences JSONB,
    audio_url VARCHAR(500),
    image_url VARCHAR(500),

    -- Elo-based difficulty
    difficulty_rating INTEGER DEFAULT 1000,

    -- Global stats
    times_shown BIGINT DEFAULT 0,
    times_correct BIGINT DEFAULT 0,

    -- Active status
    is_active BOOLEAN DEFAULT TRUE,

    -- Source tracking
    source VARCHAR(100),
    tags JSONB,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for words
CREATE INDEX idx_words_cefr_level ON words(cefr_level);
CREATE INDEX idx_words_category ON words(category);
CREATE INDEX idx_words_difficulty_rating ON words(difficulty_rating);
CREATE INDEX idx_words_word_type ON words(word_type);
CREATE INDEX idx_words_german ON words(german);

-- =============================================
-- USER WORD PROGRESS TABLE
-- =============================================

CREATE TABLE user_word_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,

    -- SM-2 Algorithm Variables
    ease_factor DECIMAL(4,2) DEFAULT 2.5 CHECK (ease_factor >= 1.3 AND ease_factor <= 5.0),
    interval_days INTEGER DEFAULT 1,
    repetition INTEGER DEFAULT 0,
    last_quality INTEGER CHECK (last_quality >= 0 AND last_quality <= 5),

    -- Review scheduling
    next_review_at TIMESTAMP,
    last_reviewed_at TIMESTAMP,

    -- Performance tracking
    times_correct INTEGER DEFAULT 0,
    times_incorrect INTEGER DEFAULT 0,
    last_response_time_ms INTEGER,
    avg_response_time_ms INTEGER,

    -- Learning status
    is_learned BOOLEAN DEFAULT FALSE,
    learned_at TIMESTAMP,
    is_favorite BOOLEAN DEFAULT FALSE,
    is_difficult BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Unique constraint
    UNIQUE(user_id, word_id)
);

-- Indexes for user_word_progress
CREATE INDEX idx_uwp_user_id ON user_word_progress(user_id);
CREATE INDEX idx_uwp_next_review ON user_word_progress(next_review_at);
CREATE INDEX idx_uwp_user_next_review ON user_word_progress(user_id, next_review_at);
CREATE INDEX idx_uwp_is_learned ON user_word_progress(is_learned);

-- =============================================
-- LEARNING SESSIONS TABLE
-- =============================================

CREATE TABLE learning_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    session_type VARCHAR(20) NOT NULL,
    cefr_level VARCHAR(2),

    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,

    words_reviewed INTEGER DEFAULT 0,
    words_correct INTEGER DEFAULT 0,
    new_words_learned INTEGER DEFAULT 0,
    total_time_seconds INTEGER,
    avg_response_time_ms INTEGER,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for learning_sessions
CREATE INDEX idx_ls_user_id ON learning_sessions(user_id);
CREATE INDEX idx_ls_started_at ON learning_sessions(started_at);

-- =============================================
-- DAILY STATS TABLE
-- =============================================

CREATE TABLE daily_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,

    words_reviewed INTEGER DEFAULT 0,
    words_correct INTEGER DEFAULT 0,
    new_words_learned INTEGER DEFAULT 0,
    sessions_completed INTEGER DEFAULT 0,
    total_time_seconds INTEGER DEFAULT 0,

    streak_maintained BOOLEAN DEFAULT FALSE,
    freeze_used BOOLEAN DEFAULT FALSE,
    xp_earned INTEGER DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Unique constraint
    UNIQUE(user_id, stat_date)
);

-- Indexes for daily_stats
CREATE INDEX idx_ds_user_id ON daily_stats(user_id);
CREATE INDEX idx_ds_stat_date ON daily_stats(stat_date);

-- =============================================
-- REFRESH TOKENS TABLE
-- =============================================

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,

    is_revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(50),

    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    device_info VARCHAR(200),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for refresh_tokens
CREATE INDEX idx_rt_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_rt_token ON refresh_tokens(token);
CREATE INDEX idx_rt_expires_at ON refresh_tokens(expires_at);

-- =============================================
-- USER PREFERENCES TABLE
-- =============================================

CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,

    -- Notification settings
    email_notifications BOOLEAN DEFAULT TRUE,
    streak_reminders BOOLEAN DEFAULT TRUE,
    weekly_report BOOLEAN DEFAULT TRUE,

    -- Learning preferences
    auto_play_audio BOOLEAN DEFAULT TRUE,
    show_example_sentences BOOLEAN DEFAULT TRUE,
    show_pronunciation BOOLEAN DEFAULT TRUE,

    -- Session settings
    words_per_session INTEGER DEFAULT 20,
    review_priority VARCHAR(20) DEFAULT 'DUE_FIRST',

    -- UI preferences
    dark_mode BOOLEAN DEFAULT FALSE,
    compact_mode BOOLEAN DEFAULT FALSE,
    keyboard_shortcuts_enabled BOOLEAN DEFAULT TRUE,
    keyboard_bindings JSONB,

    -- Reminder time
    reminder_time VARCHAR(5),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- =============================================
-- STREAK HISTORY TABLE
-- =============================================

CREATE TABLE streak_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    streak_date DATE NOT NULL,
    streak_count INTEGER NOT NULL,
    was_active BOOLEAN DEFAULT FALSE,
    freeze_used BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for streak_history
CREATE INDEX idx_sh_user_id ON streak_history(user_id);
CREATE INDEX idx_sh_date ON streak_history(streak_date);

-- =============================================
-- PASSWORD RESET TOKENS TABLE
-- =============================================

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Token stored as hash
    token_hash VARCHAR(64) NOT NULL,

    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,

    ip_address VARCHAR(50),
    user_agent VARCHAR(500),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for password_reset_tokens
CREATE INDEX idx_prt_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_prt_expires_at ON password_reset_tokens(expires_at);

-- =============================================
-- UPDATED_AT TRIGGER FUNCTION
-- =============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to all tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_words_updated_at BEFORE UPDATE ON words FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_uwp_updated_at BEFORE UPDATE ON user_word_progress FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ls_updated_at BEFORE UPDATE ON learning_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ds_updated_at BEFORE UPDATE ON daily_stats FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_rt_updated_at BEFORE UPDATE ON refresh_tokens FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_up_updated_at BEFORE UPDATE ON user_preferences FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sh_updated_at BEFORE UPDATE ON streak_history FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

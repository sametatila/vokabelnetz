-- =============================================
-- Password History Table
-- Stores hashed passwords to prevent reuse
-- =============================================

CREATE TABLE password_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Password hash (Argon2id)
    password_hash VARCHAR(255) NOT NULL,

    -- When this password was set
    created_at TIMESTAMP DEFAULT NOW()
);

-- Index for user lookup
CREATE INDEX idx_password_history_user_id ON password_history(user_id);

-- Index for cleanup (oldest first)
CREATE INDEX idx_password_history_created_at ON password_history(created_at);

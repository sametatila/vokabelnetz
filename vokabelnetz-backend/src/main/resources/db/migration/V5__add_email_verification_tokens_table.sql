-- V5: Add email verification tokens table
-- Based on SECURITY.md documentation

CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Token: 32 random bytes, hex encoded (64 chars)
    -- Stored as hash for security
    token_hash VARCHAR(64) NOT NULL,

    -- 7 day expiration (longer than password reset)
    expires_at TIMESTAMP NOT NULL,

    -- Single use
    used_at TIMESTAMP,

    -- Audit info
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_evt_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_evt_token_hash ON email_verification_tokens(token_hash);
CREATE INDEX idx_evt_expires_at ON email_verification_tokens(expires_at);

-- Trigger for updated_at
CREATE TRIGGER update_email_verification_tokens_updated_at
    BEFORE UPDATE ON email_verification_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

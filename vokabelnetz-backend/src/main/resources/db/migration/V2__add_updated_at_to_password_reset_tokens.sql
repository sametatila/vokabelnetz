-- Add missing updated_at column to password_reset_tokens table
ALTER TABLE password_reset_tokens ADD COLUMN updated_at TIMESTAMP;

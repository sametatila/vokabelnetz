-- Fix ease_factor column type from DECIMAL to REAL (float4)
ALTER TABLE user_word_progress ALTER COLUMN ease_factor TYPE REAL USING ease_factor::REAL;

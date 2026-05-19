-- META GG DB migration/fix for OAuth + existing users
-- Run this once in pgAdmin Query Tool on game_meta if the backend fails around users.provider/password_hash.

ALTER TABLE users
ADD COLUMN IF NOT EXISTS provider VARCHAR(20);

ALTER TABLE users
ADD COLUMN IF NOT EXISTS provider_id VARCHAR(100);

ALTER TABLE users
ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);

UPDATE users
SET provider = 'LOCAL'
WHERE provider IS NULL;

ALTER TABLE users
ALTER COLUMN provider SET DEFAULT 'LOCAL';

ALTER TABLE users
ALTER COLUMN password_hash DROP NOT NULL;

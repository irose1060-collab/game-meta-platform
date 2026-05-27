-- META GG DB migration/fix for OAuth + existing users
<<<<<<< HEAD
-- Run once in pgAdmin Query Tool on game_meta.
=======
-- Run this once in pgAdmin Query Tool on game_meta if the backend fails around users.provider/password_hash.
>>>>>>> abbd0112e036f567a36a11b7b79e6cb0490d82ae

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
<<<<<<< HEAD
ALTER COLUMN provider SET NOT NULL;

ALTER TABLE users
=======
>>>>>>> abbd0112e036f567a36a11b7b79e6cb0490d82ae
ALTER COLUMN password_hash DROP NOT NULL;

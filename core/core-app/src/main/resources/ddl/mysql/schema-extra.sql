ALTER TABLE users_auth
  ADD COLUMN IF NOT EXISTS active_email VARCHAR(255)
  GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN email END) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_email
ON users_auth (active_email);

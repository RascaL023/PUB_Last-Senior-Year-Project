CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_email
ON users_auth (email) WHERE deleted_at IS NULL;

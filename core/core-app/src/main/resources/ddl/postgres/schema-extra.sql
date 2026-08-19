CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_email
ON users_auth (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_active_name
ON roles (name) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_authority_active_name
ON authorities (name) WHERE deleted_at IS NULL;

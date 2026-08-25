ALTER TABLE users_auth
  ADD COLUMN IF NOT EXISTS active_email VARCHAR(255)
  GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN email END) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_email
ON users_auth (active_email);

ALTER TABLE roles
  ADD COLUMN IF NOT EXISTS active_name VARCHAR(255)
  GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name END) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_active_name
ON roles (active_name);

ALTER TABLE authorities
  ADD COLUMN IF NOT EXISTS active_name VARCHAR(255)
  GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name END) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS uk_authority_active_name
ON authorities (active_name);

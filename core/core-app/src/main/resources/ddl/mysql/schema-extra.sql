ALTER TABLE auth_users
  ADD COLUMN IF NOT EXISTS active_email VARCHAR(255)
  GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN email END) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_email
ON auth_users (active_email);

ALTER TABLE auth_roles
  ADD COLUMN IF NOT EXISTS active_name VARCHAR(255)
  GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name END) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_active_name
ON auth_roles (active_name);

ALTER TABLE auth_authorities
  ADD COLUMN IF NOT EXISTS active_name VARCHAR(255)
  GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN name END) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS uk_authority_active_name
ON auth_authorities (active_name);

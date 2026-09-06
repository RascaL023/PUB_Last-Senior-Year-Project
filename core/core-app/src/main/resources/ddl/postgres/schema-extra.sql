CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_email
ON auth_users (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_active_name
ON auth_roles (name) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_authority_active_name
ON auth_authorities (name) WHERE deleted_at IS NULL;

-- Report dashboard aggregation (Phase 3)
CREATE INDEX IF NOT EXISTS idx_orders_created_paid
ON orders (created_at, paid_status);

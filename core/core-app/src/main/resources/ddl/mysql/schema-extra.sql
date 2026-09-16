-- Catatan (B13): index report paralel dengan ddl/postgres/schema-extra.sql.
-- MySQL tidak mendukung partial index seperti Postgres, jadi unique-index "aktif"
-- memakai generated column (pola yang sama dengan bagian auth di atas).

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

-- ── Report dashboard (pembaca langsung, tanpa tabel projection) ───────────────

-- Basis tagihan: invoice yang dilunasi pada periode (fakta paid_at).
CREATE INDEX IF NOT EXISTS idx_invoices_paid_at
ON invoices (paid_at);

-- Piutang berjalan: invoice OPEN / PARTIALLY_PAID.
CREATE INDEX IF NOT EXISTS idx_invoices_status
ON invoices (status);

-- Baris tagihan: agregasi menu (per invoice) dan pencarian status billing per order.
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id
ON invoice_items (invoice_id);

CREATE INDEX IF NOT EXISTS idx_invoice_items_order_id
ON invoice_items (order_id);

-- Aktivitas terbaru dashboard (order terbaru, tanpa filter periode).
CREATE INDEX IF NOT EXISTS idx_orders_created_at
ON orders (created_at);

-- Basis kas: uang masuk per periode (paid_at).
CREATE INDEX IF NOT EXISTS idx_payments_paid_at
ON payments (paid_at);

-- Pencarian payment per tagihan (filter invoiceId di list payments).
CREATE INDEX IF NOT EXISTS idx_payments_invoice_id
ON payments (invoice_id);

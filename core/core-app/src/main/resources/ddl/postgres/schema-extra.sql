CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_email
ON auth_users (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_active_name
ON auth_roles (name) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_authority_active_name
ON auth_authorities (name) WHERE deleted_at IS NULL;

-- ── Report dashboard (pembaca langsung, tanpa tabel projection) ───────────────
-- Basis tagihan: invoice yang dilunasi pada periode (fakta paid_at).
CREATE INDEX IF NOT EXISTS idx_invoices_paid_at
ON invoices (paid_at);

-- Piutang berjalan: invoice OPEN / PARTIALLY_PAID.
CREATE INDEX IF NOT EXISTS idx_invoices_status
ON invoices (status);

-- Koreksi tagihan per periode.
CREATE INDEX IF NOT EXISTS idx_refunds_created_at
ON refunds (created_at);

-- Baris tagihan: agregasi menu (per invoice) dan pencarian status billing per order.
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id
ON invoice_items (invoice_id);

CREATE INDEX IF NOT EXISTS idx_invoice_items_order_id
ON invoice_items (order_id);

-- Aktivitas terbaru dashboard (order terbaru, tanpa filter periode).
CREATE INDEX IF NOT EXISTS idx_orders_created_at
ON orders (created_at);

-- Basis kas: uang masuk (paid_at) dan uang keluar (refunded_at) per periode.
CREATE INDEX IF NOT EXISTS idx_payments_paid_at
ON payments (paid_at);

CREATE INDEX IF NOT EXISTS idx_payments_refunded_at
ON payments (refunded_at);

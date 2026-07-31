CREATE INDEX IF NOT EXISTS idx_payments_user_created_at
    ON payments (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payments_status_updated_at
    ON payments (status, updated_at);

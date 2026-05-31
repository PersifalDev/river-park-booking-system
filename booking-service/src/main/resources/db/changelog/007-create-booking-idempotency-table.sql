CREATE TABLE IF NOT EXISTS booking_idempotency_key
(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    booking_id UUID NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_booking_idempotency_user_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_booking_idempotency_expires_at
    ON booking_idempotency_key (expires_at);

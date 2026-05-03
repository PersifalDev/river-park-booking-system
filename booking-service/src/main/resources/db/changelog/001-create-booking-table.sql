CREATE EXTENSION IF NOT EXISTS pgcrypto;


CREATE TABLE booking
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             BIGINT NOT NULL,
    room_category_id    BIGINT NOT NULL,
    booking_code        VARCHAR(64) NOT NULL UNIQUE,
    guests              INT NOT NULL,
    adult_count         INT NOT NULL,
    children_count      INT NOT NULL,
    check_in_date       DATE NOT NULL,
    check_out_date      DATE NOT NULL,
    price_amount        NUMERIC(12, 2) NOT NULL,
    hold_expires_at     TIMESTAMPTZ,
    has_promo           BOOLEAN NOT NULL,
    status              INT NOT NULL DEFAULT 1,
    cancellation_reason TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_booking_user_id ON booking(user_id);
CREATE INDEX IF NOT EXISTS idx_booking_status ON booking(status);
CREATE INDEX IF NOT EXISTS idx_booking_hold_expires_at ON booking(hold_expires_at);
CREATE INDEX IF NOT EXISTS idx_booking_created_at ON booking(created_at);

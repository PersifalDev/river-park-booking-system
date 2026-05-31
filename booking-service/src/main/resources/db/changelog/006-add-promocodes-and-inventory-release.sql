ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS inventory_released_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS applied_promo_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS generated_promo_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS promo_discount_percent INT;

CREATE TABLE IF NOT EXISTS promo_code
(
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(64) NOT NULL UNIQUE,
    user_id             BIGINT NOT NULL,
    source_booking_id   UUID NOT NULL,
    redeemed_booking_id UUID,
    discount_percent    INT NOT NULL,
    used                BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL,
    redeemed_at         TIMESTAMPTZ,
    CONSTRAINT chk_promo_code_discount_percent CHECK (discount_percent > 0 AND discount_percent < 100)
);

CREATE INDEX IF NOT EXISTS idx_promo_code_user_used ON promo_code(user_id, used);
CREATE UNIQUE INDEX IF NOT EXISTS uk_promo_code_source_booking_id ON promo_code(source_booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_inventory_released_at ON booking(inventory_released_at);
CREATE INDEX IF NOT EXISTS idx_booking_check_out_date ON booking(check_out_date);

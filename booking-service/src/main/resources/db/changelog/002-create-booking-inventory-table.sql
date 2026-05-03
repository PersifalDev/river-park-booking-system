CREATE TABLE booking_inventory
(
    id              BIGSERIAL PRIMARY KEY,
    room_category_id BIGINT NOT NULL,
    booking_date    DATE NOT NULL,
    total_units     INT NOT NULL,
    held_units      INT NOT NULL DEFAULT 0,
    confirmed_units INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_booking_inventory_category_date UNIQUE (room_category_id, booking_date)
);

CREATE INDEX IF NOT EXISTS idx_booking_inventory_category_date ON booking_inventory(room_category_id, booking_date);

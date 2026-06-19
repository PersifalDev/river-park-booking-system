CREATE TABLE booking_room
(
    id                  BIGSERIAL PRIMARY KEY,
    room_category_id    BIGINT NOT NULL,
    room_number         VARCHAR(32) NOT NULL,
    floor               INT NOT NULL,
    status              VARCHAR(32) NOT NULL,
    housekeeping_status VARCHAR(32) NOT NULL,
    maintenance_note    TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_booking_room_number UNIQUE (room_number),
    CONSTRAINT chk_booking_room_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'OUT_OF_ORDER', 'BLOCKED')),
    CONSTRAINT chk_booking_room_housekeeping_status CHECK (housekeeping_status IN ('CLEAN', 'DIRTY', 'INSPECTED'))
);

CREATE INDEX IF NOT EXISTS idx_booking_room_category_status ON booking_room(room_category_id, status);
CREATE INDEX IF NOT EXISTS idx_booking_room_floor_number ON booking_room(floor, room_number);

CREATE TABLE booking_room_block
(
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT NOT NULL REFERENCES booking_room(id) ON DELETE CASCADE,
    from_date  DATE NOT NULL,
    to_date    DATE NOT NULL,
    reason     VARCHAR(32) NOT NULL,
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_booking_room_block_date_range CHECK (to_date > from_date),
    CONSTRAINT chk_booking_room_block_reason CHECK (reason IN ('MAINTENANCE', 'OUT_OF_ORDER', 'DEEP_CLEANING', 'OWNER_BLOCK', 'ADMIN_BLOCK'))
);

CREATE INDEX IF NOT EXISTS idx_booking_room_block_room_dates ON booking_room_block(room_id, from_date, to_date);

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS room_id BIGINT REFERENCES booking_room(id);

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS room_number_snapshot VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_booking_room_id ON booking(room_id);
CREATE INDEX IF NOT EXISTS idx_booking_room_dates_status ON booking(room_id, check_in_date, check_out_date, status);

WITH category_plan(room_category_id, prefix, floor_from, room_count) AS (
    VALUES
        (1::BIGINT, 'S', 2, 90),
        (2::BIGINT, 'D', 4, 120),
        (3::BIGINT, 'P', 7, 40),
        (4::BIGINT, 'T', 8, 28),
        (5::BIGINT, 'B', 9, 14),
        (6::BIGINT, 'E', 1, 20)
),
rooms AS (
    SELECT
        room_category_id,
        prefix || lpad(number_index::TEXT, 3, '0') AS room_number,
        floor_from + ((number_index - 1) / 20) AS floor
    FROM category_plan
    CROSS JOIN LATERAL generate_series(1, room_count) AS number_index
)
INSERT INTO booking_room(room_category_id, room_number, floor, status, housekeeping_status, created_at, updated_at)
SELECT room_category_id, room_number, floor, 'ACTIVE', 'CLEAN', now(), now()
FROM rooms
ON CONFLICT (room_number) DO NOTHING;

INSERT INTO booking_room_block(room_id, from_date, to_date, reason, comment, created_at, updated_at)
SELECT id, CURRENT_DATE, CURRENT_DATE + 14, 'MAINTENANCE', 'Плановое обслуживание номера', now(), now()
FROM booking_room
WHERE room_number = 'S001'
ON CONFLICT DO NOTHING;

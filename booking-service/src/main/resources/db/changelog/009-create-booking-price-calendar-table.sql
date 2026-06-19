CREATE TABLE booking_price_calendar
(
    id                  BIGSERIAL PRIMARY KEY,
    room_category_id    BIGINT NOT NULL,
    calendar_date       DATE NOT NULL,
    rate_plan_id        BIGINT NOT NULL REFERENCES booking_tariff(id),
    price               NUMERIC(12, 2) NOT NULL,
    available           BOOLEAN NOT NULL DEFAULT TRUE,
    min_stay            INT,
    closed_to_arrival   BOOLEAN NOT NULL DEFAULT FALSE,
    closed_to_departure BOOLEAN NOT NULL DEFAULT FALSE,
    season_code         VARCHAR(64),
    promotion_code      VARCHAR(64),
    holiday_name        VARCHAR(128),
    occupancy_percent   NUMERIC(5, 2),
    demand_multiplier   NUMERIC(8, 4) NOT NULL DEFAULT 1.0000,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_booking_price_calendar_category_date_rate_plan
        UNIQUE (room_category_id, calendar_date, rate_plan_id),
    CONSTRAINT chk_booking_price_calendar_price_non_negative
        CHECK (price >= 0),
    CONSTRAINT chk_booking_price_calendar_min_stay_positive
        CHECK (min_stay IS NULL OR min_stay > 0),
    CONSTRAINT chk_booking_price_calendar_occupancy_range
        CHECK (occupancy_percent IS NULL OR (occupancy_percent >= 0 AND occupancy_percent <= 100)),
    CONSTRAINT chk_booking_price_calendar_multiplier_positive
        CHECK (demand_multiplier > 0)
);

CREATE INDEX IF NOT EXISTS idx_booking_price_calendar_lookup
    ON booking_price_calendar(room_category_id, rate_plan_id, calendar_date);

CREATE INDEX IF NOT EXISTS idx_booking_price_calendar_date
    ON booking_price_calendar(calendar_date);

INSERT INTO booking_price_calendar (
    room_category_id,
    calendar_date,
    rate_plan_id,
    price,
    available,
    min_stay,
    closed_to_arrival,
    closed_to_departure,
    season_code,
    promotion_code,
    holiday_name,
    occupancy_percent,
    demand_multiplier
)
SELECT room_category_id,
       calendar_date,
       tariff.id,
       price,
       available,
       min_stay,
       closed_to_arrival,
       closed_to_departure,
       season_code,
       promotion_code,
       holiday_name,
       occupancy_percent,
       demand_multiplier
FROM (
    VALUES
        (1, DATE '2026-06-20', 'ROOM_ONLY', 6200.00, TRUE, 1, FALSE, FALSE, 'SUMMER', NULL, NULL, 72.00, 1.0800),
        (1, DATE '2026-06-21', 'ROOM_ONLY', 5900.00, TRUE, 1, FALSE, FALSE, 'SUMMER', 'SUNDAY_VALUE', NULL, 64.00, 1.0000),
        (1, DATE '2026-06-22', 'ROOM_ONLY', 5400.00, TRUE, 1, FALSE, FALSE, 'SUMMER', NULL, NULL, 52.00, 0.9500),
        (1, DATE '2026-06-20', 'BREAKFAST', 6200.00, TRUE, 1, FALSE, FALSE, 'SUMMER', NULL, NULL, 72.00, 1.0800),
        (1, DATE '2026-06-21', 'BREAKFAST', 5900.00, TRUE, 1, FALSE, FALSE, 'SUMMER', 'SUNDAY_VALUE', NULL, 64.00, 1.0000),
        (1, DATE '2026-06-22', 'BREAKFAST', 5400.00, TRUE, 1, FALSE, FALSE, 'SUMMER', NULL, NULL, 52.00, 0.9500),
        (2, DATE '2026-06-20', 'ROOM_ONLY', 7900.00, TRUE, 2, FALSE, FALSE, 'SUMMER_WEEKEND', NULL, NULL, 81.00, 1.1500),
        (2, DATE '2026-06-21', 'ROOM_ONLY', 7600.00, TRUE, 2, FALSE, FALSE, 'SUMMER_WEEKEND', NULL, NULL, 79.00, 1.1000),
        (2, DATE '2026-06-22', 'ROOM_ONLY', 6800.00, TRUE, 1, FALSE, FALSE, 'SUMMER', NULL, NULL, 58.00, 1.0000)
) AS seed(room_category_id, calendar_date, tariff_code, price, available, min_stay, closed_to_arrival, closed_to_departure, season_code, promotion_code, holiday_name, occupancy_percent, demand_multiplier)
JOIN booking_tariff tariff ON tariff.code = seed.tariff_code;

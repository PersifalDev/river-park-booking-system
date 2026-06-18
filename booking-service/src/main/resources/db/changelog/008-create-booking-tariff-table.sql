CREATE TABLE booking_tariff
(
    id                              BIGSERIAL PRIMARY KEY,
    code                            VARCHAR(64) NOT NULL UNIQUE,
    title                           VARCHAR(128) NOT NULL,
    description                     TEXT,
    price_modifier_type             VARCHAR(32) NOT NULL,
    price_modifier_value            NUMERIC(10, 2) NOT NULL,
    min_nights                      INT,
    max_nights                      INT,
    min_adults                      INT,
    min_children                    INT,
    active_from                     DATE,
    active_to                       DATE,
    cancellation_policy             VARCHAR(32) NOT NULL,
    free_cancellation_days_before   INT,
    included_services               TEXT,
    sort_order                      INT NOT NULL DEFAULT 100,
    active                          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_booking_tariff_active ON booking_tariff(active);
CREATE INDEX IF NOT EXISTS idx_booking_tariff_code ON booking_tariff(code);

ALTER TABLE booking
    ADD COLUMN tariff_code VARCHAR(64),
    ADD COLUMN tariff_title VARCHAR(128),
    ADD COLUMN tariff_cancellation_policy VARCHAR(32),
    ADD COLUMN tariff_free_cancellation_days_before INT,
    ADD COLUMN tariff_included_services TEXT;

UPDATE booking
SET tariff_code = 'ROOM_ONLY',
    tariff_title = 'Без завтрака',
    tariff_cancellation_policy = 'FLEXIBLE',
    tariff_free_cancellation_days_before = 1,
    tariff_included_services = 'Проживание';

ALTER TABLE booking
    ALTER COLUMN tariff_code SET NOT NULL,
    ALTER COLUMN tariff_title SET NOT NULL,
    ALTER COLUMN tariff_cancellation_policy SET NOT NULL;

INSERT INTO booking_tariff (
    code,
    title,
    description,
    price_modifier_type,
    price_modifier_value,
    min_nights,
    max_nights,
    min_adults,
    min_children,
    active_from,
    active_to,
    cancellation_policy,
    free_cancellation_days_before,
    included_services,
    sort_order,
    active
) VALUES
('ROOM_ONLY', 'Без завтрака', 'Базовый тариф для гостей, которым нужен только номер.', 'PERCENT', 0.00, 1, NULL, 1, NULL, NULL, NULL, 'FLEXIBLE', 1, 'Проживание', 10, TRUE),
('BREAKFAST', 'С завтраком', 'Проживание с завтраком для всех гостей.', 'FIXED_PER_NIGHT', 900.00, 1, NULL, 1, NULL, NULL, NULL, 'FLEXIBLE', 1, 'Проживание; завтрак', 20, TRUE),
('FAMILY', 'Семейный', 'Пакет для отдыха с детьми: завтрак и детские сервисы.', 'FIXED_PER_NIGHT', 1600.00, 1, NULL, 1, 1, NULL, NULL, 'FREE_UNTIL_DEADLINE', 2, 'Проживание; завтрак; детская зона; поздний выезд при наличии мест', 30, TRUE),
('AQUAPARK', 'Аквапарк', 'Пакет проживания с посещением аквапарка.', 'FIXED_PER_STAY', 4500.00, 1, NULL, 1, NULL, NULL, NULL, 'STRICT', 3, 'Проживание; билеты в аквапарк; завтрак', 40, TRUE),
('ALL_INCLUSIVE', 'Всё включено', 'Расширенный пакет с питанием и дополнительными услугами.', 'PERCENT', 35.00, 1, NULL, 1, NULL, NULL, NULL, 'FREE_UNTIL_DEADLINE', 3, 'Проживание; завтрак; ужин; парковка; поздний выезд при наличии мест', 50, TRUE),
('HAPPY_DAYS', 'Счастливые дни', 'Специальная цена на короткие заезды в период акции.', 'PERCENT', -12.00, 1, 3, 1, NULL, DATE '2026-01-01', DATE '2026-12-31', 'STRICT', 0, 'Проживание по специальной цене', 60, TRUE),
('NON_REFUNDABLE', 'Невозвратный', 'Лучшая цена при полной предоплате без возврата.', 'PERCENT', -15.00, 1, NULL, 1, NULL, NULL, NULL, 'NON_REFUNDABLE', 0, 'Проживание; фиксированная цена', 70, TRUE),
('FREE_CANCEL', 'Бесплатная отмена', 'Гибкий тариф с бесплатной отменой до дедлайна.', 'PERCENT', 8.00, 1, NULL, 1, NULL, NULL, NULL, 'FREE_UNTIL_DEADLINE', 2, 'Проживание; бесплатная отмена до дедлайна', 80, TRUE),
('ROMANTIC_WEEKEND', 'Романтический выходной', 'Пакет для двух гостей с завтраком и комплиментом.', 'FIXED_PER_STAY', 3500.00, 1, 2, 2, NULL, NULL, NULL, 'FREE_UNTIL_DEADLINE', 2, 'Проживание; завтрак; комплимент в номер; поздний выезд при наличии мест', 90, TRUE),
('LONG_STAY', 'Длительное проживание', 'Скидка на заезды от семи ночей.', 'PERCENT', -15.00, 7, NULL, 1, NULL, NULL, NULL, 'FLEXIBLE', 3, 'Проживание; скидка за длительный заезд', 100, TRUE);

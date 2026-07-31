ALTER TABLE booking_outbox
    ADD COLUMN event_kind VARCHAR(32) NOT NULL DEFAULT 'BOOKING';

ALTER TABLE booking_outbox
    ADD CONSTRAINT chk_booking_outbox_event_kind
        CHECK (event_kind IN ('BOOKING', 'NOTIFICATION'));

CREATE INDEX idx_booking_outbox_event_kind_status
    ON booking_outbox (event_kind, status, next_attempt_at);

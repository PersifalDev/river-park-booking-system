ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS hold_reminder_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS check_in_reminder_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_booking_hold_reminder_sent_at ON booking(hold_reminder_sent_at);
CREATE INDEX IF NOT EXISTS idx_booking_check_in_reminder_sent_at ON booking(check_in_reminder_sent_at);
CREATE INDEX IF NOT EXISTS idx_booking_check_in_date_status ON booking(check_in_date, status);

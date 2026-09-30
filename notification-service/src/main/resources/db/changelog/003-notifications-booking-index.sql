CREATE INDEX IF NOT EXISTS idx_notifications_user_booking_created
    ON notifications (user_id, booking_id, created_at DESC);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    booking_id UUID,
    payment_id UUID,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(80) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id_created_at ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id_is_read_created_at ON notifications (user_id, is_read, created_at DESC);
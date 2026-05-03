CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    booking_code VARCHAR(120) NOT NULL,
    user_id BIGINT NOT NULL,
    price_amount NUMERIC(14, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(120) NOT NULL,
    payment_comment TEXT NOT NULL,
    contact_phone VARCHAR(120) NOT NULL,
    payment_instruction TEXT NOT NULL,
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
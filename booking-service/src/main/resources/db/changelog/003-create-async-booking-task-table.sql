CREATE TABLE async_booking_task
(
    id              BIGSERIAL PRIMARY KEY,
    booking_id      UUID NOT NULL,
    task_status     INT NOT NULL,
    processing_step INT NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_async_booking_task_status ON async_booking_task(task_status);
CREATE INDEX IF NOT EXISTS idx_async_booking_task_next_attempt_at ON async_booking_task(next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_async_booking_task_booking_id ON async_booking_task(booking_id);

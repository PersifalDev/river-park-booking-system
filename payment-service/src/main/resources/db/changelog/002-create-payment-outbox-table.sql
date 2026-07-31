CREATE TABLE payment_outbox
(
    id              UUID                     NOT NULL,
    aggregate_id    UUID                     NOT NULL,
    event_type      VARCHAR(128)             NOT NULL,
    payload         JSONB                    NOT NULL,
    status          VARCHAR(32)              NOT NULL,
    attempts        INTEGER                  NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    sent_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_payment_outbox PRIMARY KEY (id),
    CONSTRAINT chk_payment_outbox_status CHECK (status IN ('NEW', 'PROCESSING', 'SENT', 'FAILED')),
    CONSTRAINT chk_payment_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_payment_outbox_status_next_attempt_created
    ON payment_outbox (status, next_attempt_at, created_at);

CREATE INDEX idx_payment_outbox_aggregate_id
    ON payment_outbox (aggregate_id);

CREATE TABLE processed_event
(
    id            BIGSERIAL                NOT NULL,
    event_id      UUID                     NOT NULL,
    consumer_name VARCHAR(128)             NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_notification_processed_event PRIMARY KEY (id),
    CONSTRAINT uk_notification_processed_event UNIQUE (event_id, consumer_name)
);

CREATE INDEX idx_notification_processed_event_processed_at
    ON processed_event (processed_at);

CREATE TABLE user_consents
(
    id           UUID PRIMARY KEY,
    user_id      BIGINT                   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    consent_type VARCHAR(64)              NOT NULL,
    version      VARCHAR(64)              NOT NULL,
    accepted_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_user_consents_user_id ON user_consents (user_id);
CREATE UNIQUE INDEX uq_user_consents_user_type_version ON user_consents (user_id, consent_type, version);

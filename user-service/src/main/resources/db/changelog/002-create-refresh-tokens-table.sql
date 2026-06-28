CREATE TABLE refresh_tokens
(
    id             UUID PRIMARY KEY,
    user_id        BIGINT                   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash     VARCHAR(64)              NOT NULL UNIQUE,
    family_id      UUID                     NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at     TIMESTAMP WITH TIME ZONE,
    replaced_by_id UUID REFERENCES refresh_tokens (id),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at   TIMESTAMP WITH TIME ZONE,
    user_agent     VARCHAR(256),
    ip_address     VARCHAR(64)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

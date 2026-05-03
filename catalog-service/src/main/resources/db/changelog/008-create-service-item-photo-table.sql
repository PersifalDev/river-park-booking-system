CREATE TABLE IF NOT EXISTS service_item_photo (
    id BIGSERIAL PRIMARY KEY,
    service_item_id BIGINT NOT NULL UNIQUE REFERENCES service_item(id) ON DELETE CASCADE,
    path VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_service_item_photo_service_item_id ON service_item_photo(service_item_id);

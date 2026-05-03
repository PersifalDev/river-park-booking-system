ALTER TABLE room_category_photo
    ADD COLUMN sort_order INTEGER;

UPDATE room_category_photo
SET sort_order = id
WHERE sort_order IS NULL;

ALTER TABLE room_category_photo
    ALTER COLUMN sort_order SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_room_category_photo_category_sort_order
    ON room_category_photo(room_category_id, sort_order);
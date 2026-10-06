ALTER TABLE noghterangi.media
    ADD COLUMN category_id BIGINT REFERENCES noghterangi.category(id) ON DELETE RESTRICT;

ALTER TABLE noghterangi.media
    ALTER COLUMN product_id DROP NOT NULL;

ALTER TABLE noghterangi.media
    ADD CONSTRAINT media_single_owner_check CHECK (
        (product_id IS NOT NULL AND category_id IS NULL)
        OR (product_id IS NULL AND category_id IS NOT NULL)
    );

CREATE INDEX media_category_idx ON noghterangi.media(category_id);
CREATE UNIQUE INDEX media_one_category_image_idx
    ON noghterangi.media(category_id)
    WHERE category_id IS NOT NULL;

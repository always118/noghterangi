ALTER TABLE noghterangi.category
    ADD COLUMN parent_id BIGINT,
    ADD CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES noghterangi.category(id),
    ADD CONSTRAINT ck_category_not_own_parent CHECK (parent_id <> id);

-- Supports parent lookup and checking whether a category has children.
CREATE INDEX idx_category_parent ON noghterangi.category(parent_id);

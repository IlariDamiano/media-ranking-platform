
CREATE INDEX idx_media_title_trgm
    ON media
    USING GIN (lower(title) gin_trgm_ops);

CREATE INDEX idx_media_aliases_title_trgm
    ON media_aliases
    USING GIN (lower(title) gin_trgm_ops);

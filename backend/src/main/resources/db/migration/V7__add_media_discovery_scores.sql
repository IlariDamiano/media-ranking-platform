
ALTER TABLE media
    ADD COLUMN popularity_score DOUBLE PRECISION,
    ADD COLUMN external_rating DOUBLE PRECISION,
    ADD COLUMN external_rating_count INTEGER;

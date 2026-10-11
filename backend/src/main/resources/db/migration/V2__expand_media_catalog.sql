-- V2: Expand the shared media catalog.

-- Support all six media categories.
ALTER TABLE media
DROP CONSTRAINT chk_media_type;

ALTER TABLE media
    ADD CONSTRAINT chk_media_type
        CHECK (
            media_type IN (
                           'MOVIE',
                           'TV_SHOW',
                           'ANIME',
                           'MANGA',
                           'MANHWA',
                           'MANHUA'
                )
            );

-- Add shared catalog metadata.
ALTER TABLE media
    ADD COLUMN format VARCHAR(30),
    ADD COLUMN release_status VARCHAR(30),
    ADD COLUMN end_date DATE,
    ADD COLUMN release_year SMALLINT,
    ADD COLUMN original_language VARCHAR(15),
    ADD COLUMN origin_country CHAR(2),
    ADD COLUMN is_adult BOOLEAN;

-- Validate the new metadata.
ALTER TABLE media
    ADD CONSTRAINT chk_media_format
        CHECK (
            format IS NULL OR length(trim(format)) > 0
            );

ALTER TABLE media
    ADD CONSTRAINT chk_media_release_year
        CHECK (
            release_year IS NULL
                OR release_year BETWEEN 1800 AND 9999
            );

ALTER TABLE media
    ADD CONSTRAINT chk_media_release_dates
        CHECK (
            end_date IS NULL
                OR release_date IS NULL
                OR end_date >= release_date
            );

ALTER TABLE media
    ADD CONSTRAINT chk_media_release_status
        CHECK (
            release_status IS NULL
                OR release_status IN (
                                      'ANNOUNCED',
                                      'UPCOMING',
                                      'ONGOING',
                                      'COMPLETED',
                                      'HIATUS',
                                      'CANCELLED'
                )
            );

-- Add indexes for common catalog queries.
CREATE INDEX idx_media_type ON media (media_type);

CREATE INDEX idx_media_release_date
    ON media (release_date DESC)
    WHERE release_date IS NOT NULL;
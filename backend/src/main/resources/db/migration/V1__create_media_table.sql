CREATE TABLE media (
                       id UUID PRIMARY KEY DEFAULT uuidv7(),

                       title VARCHAR(300) NOT NULL,
                       media_type VARCHAR(20) NOT NULL,

                       description TEXT,
                       release_date DATE,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       CONSTRAINT chk_media_type
                           CHECK (media_type IN ('MOVIE', 'TV_SHOW', 'MANGA')),

                       CONSTRAINT chk_media_title
                           CHECK (length(trim(title)) > 0)
);
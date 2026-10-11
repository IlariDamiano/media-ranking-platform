
CREATE TABLE media_aliases (
                               id UUID PRIMARY KEY,
                               media_id UUID NOT NULL REFERENCES media(id)
                                   ON DELETE CASCADE,
                               title VARCHAR(500) NOT NULL,
                               language_code VARCHAR(15),
                               alias_type VARCHAR(30) NOT NULL,

                               CONSTRAINT chk_media_alias_title_not_blank
                                   CHECK (length(trim(title)) > 0),

                               CONSTRAINT uq_media_alias
                                   UNIQUE (media_id, title, alias_type)
);

CREATE INDEX idx_media_aliases_media_id
    ON media_aliases(media_id);

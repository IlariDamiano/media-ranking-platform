-- V3: Add multilingual and alternative titles to media.

CREATE TABLE media_titles (
                              id UUID PRIMARY KEY,
                              media_id UUID NOT NULL,

                              title VARCHAR(500) NOT NULL,
                              language_code VARCHAR(20),
                              title_type VARCHAR(30) NOT NULL,

                              CONSTRAINT fk_media_titles_media
                                  FOREIGN KEY (media_id)
                                      REFERENCES media(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT chk_media_titles_type
                                  CHECK (
                                      title_type IN (
                                                     'PRIMARY',
                                                     'ENGLISH',
                                                     'ITALIAN',
                                                     'ORIGINAL',
                                                     'ROMANIZED',
                                                     'ALTERNATIVE'
                                          )
                                      ),

                              CONSTRAINT chk_media_titles_not_blank
                                  CHECK (LENGTH(TRIM(title)) > 0)
);
-- Store identifiers assigned by external media providers.

CREATE TABLE media_external_ids (
                                    id UUID PRIMARY KEY,
                                    media_id UUID NOT NULL,

                                    provider VARCHAR(30) NOT NULL,
                                    provider_media_type VARCHAR(30) NOT NULL,
                                    external_id VARCHAR(100) NOT NULL,

                                    CONSTRAINT fk_media_external_ids_media
                                        FOREIGN KEY (media_id)
                                            REFERENCES media(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT uq_media_external_identity
                                        UNIQUE (provider, provider_media_type, external_id),

                                    CONSTRAINT chk_media_external_id_not_blank
                                        CHECK (LENGTH(TRIM(external_id)) > 0)
);

CREATE INDEX idx_media_external_ids_media
    ON media_external_ids (media_id);

-- Represent a collection of related media entries.
-- Example: the main Attack on Titan anime hub.

CREATE TABLE media_groups (
                              id UUID PRIMARY KEY,

                              name VARCHAR(500) NOT NULL,
                              description TEXT,

                              group_type VARCHAR(30) NOT NULL,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                              CONSTRAINT chk_media_groups_name
                                  CHECK (LENGTH(TRIM(name)) > 0),

                              CONSTRAINT chk_media_groups_type
                                  CHECK (
                                      group_type IN (
                                                     'SERIES',
                                                     'FRANCHISE',
                                                     'COLLECTION'
                                          )
                                      )
);

-- Connect media entries to their Media Hubs.

CREATE TABLE media_group_members (
                                     id UUID PRIMARY KEY,

                                     group_id UUID NOT NULL,
                                     media_id UUID NOT NULL,

                                     section VARCHAR(30) NOT NULL,
                                     display_order INTEGER NOT NULL,

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                     CONSTRAINT fk_group_members_group
                                         FOREIGN KEY (group_id)
                                             REFERENCES media_groups(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT fk_group_members_media
                                         FOREIGN KEY (media_id)
                                             REFERENCES media(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT uq_group_members
                                         UNIQUE (group_id, media_id),

                                     CONSTRAINT chk_group_members_section
                                         CHECK (
                                             section IN (
                                             'MAIN_STORY',
                                             'MOVIES',
                                             'EXTRAS',
                                             'SPIN_OFFS',
                                             'RELATED'
                                             )
),

    CONSTRAINT chk_group_members_order
        CHECK (display_order >= 0)
);

CREATE INDEX idx_group_members_media
    ON media_group_members (media_id);

CREATE INDEX idx_group_members_order
    ON media_group_members (group_id, section, display_order);

-- Store relationships between individual media entries.
-- Examples: sequels, prequels, adaptations and side stories.

CREATE TABLE media_relations (
                                 id UUID PRIMARY KEY,

                                 source_media_id UUID NOT NULL,
                                 target_media_id UUID NOT NULL,

                                 relation_type VARCHAR(30) NOT NULL,

                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                 CONSTRAINT fk_media_relations_source
                                     FOREIGN KEY (source_media_id)
                                         REFERENCES media(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_media_relations_target
                                     FOREIGN KEY (target_media_id)
                                         REFERENCES media(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT uq_media_relation
                                     UNIQUE (
                                             source_media_id,
                                             target_media_id,
                                             relation_type
                                         ),

                                 CONSTRAINT chk_media_relation_not_self
                                     CHECK (source_media_id <> target_media_id),

                                 CONSTRAINT chk_media_relation_type
                                     CHECK (
                                         relation_type IN (
                                                           'SEQUEL',
                                                           'PREQUEL',
                                                           'ADAPTATION',
                                                           'SOURCE',
                                                           'SIDE_STORY',
                                                           'SPIN_OFF',
                                                           'ALTERNATIVE',
                                                           'SUMMARY',
                                                           'CHARACTER',
                                                           'OTHER'
                                             )
                                         )
);

CREATE INDEX idx_media_relations_target
    ON media_relations (target_media_id);

-- Speed up retrieving titles for a media entry.
CREATE INDEX idx_media_titles_media
    ON media_titles (media_id);

-- Support case-insensitive title searches.
CREATE INDEX idx_media_titles_lower_title
    ON media_titles (LOWER(title));

-- Store multilingual and alternative names for Media Hubs.

CREATE TABLE media_group_titles (
                                    id UUID PRIMARY KEY,

                                    group_id UUID NOT NULL,
                                    title VARCHAR(500) NOT NULL,
                                    language_code VARCHAR(20),
                                    title_type VARCHAR(30) NOT NULL,

                                    CONSTRAINT fk_media_group_titles_group
                                        FOREIGN KEY (group_id)
                                            REFERENCES media_groups(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT chk_media_group_titles_not_blank
                                        CHECK (LENGTH(TRIM(title)) > 0),

                                    CONSTRAINT chk_media_group_titles_type
                                        CHECK (
                                            title_type IN (
                                                           'PRIMARY',
                                                           'ENGLISH',
                                                           'ITALIAN',
                                                           'ORIGINAL',
                                                           'ROMANIZED',
                                                           'ALTERNATIVE'
                                                )
                                            )
);

CREATE INDEX idx_media_group_titles_group
    ON media_group_titles (group_id);

CREATE INDEX idx_media_group_titles_lower_title
    ON media_group_titles (LOWER(title));

-- Enable trigram-based partial and fuzzy searching.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Improve searching individual media titles.
CREATE INDEX idx_media_titles_trgm
    ON media_titles
    USING GIN (LOWER(title) gin_trgm_ops);

-- Improve searching Media Hub titles.
CREATE INDEX idx_media_group_titles_trgm
    ON media_group_titles
    USING GIN (LOWER(title) gin_trgm_ops);

-- Identify the primary category of a Media Hub.
ALTER TABLE media_groups
    ADD COLUMN primary_media_type VARCHAR(20);

ALTER TABLE media_groups
    ADD CONSTRAINT chk_media_groups_primary_type
        CHECK (
            primary_media_type IS NULL OR
            primary_media_type IN (
                                   'MOVIE',
                                   'TV_SHOW',
                                   'ANIME',
                                   'MANGA',
                                   'MANHWA',
                                   'MANHUA'
                )
            );
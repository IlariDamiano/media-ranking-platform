
CREATE TABLE user_lists (
                            id UUID PRIMARY KEY,

                            owner_id UUID NOT NULL,
                            name VARCHAR(120) NOT NULL,
                            description TEXT,
                            visibility VARCHAR(10) NOT NULL DEFAULT 'PRIVATE',

                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                            CONSTRAINT fk_user_lists_owner
                                FOREIGN KEY (owner_id)
                                    REFERENCES app_users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT chk_user_lists_name
                                CHECK (length(trim(name)) > 0),

                            CONSTRAINT chk_user_lists_description
                                CHECK (
                                    description IS NULL
                                        OR length(description) <= 2000
                                    ),

                            CONSTRAINT chk_user_lists_visibility
                                CHECK (visibility IN ('PRIVATE', 'PUBLIC'))
);

CREATE TABLE user_list_items (
                                 id UUID PRIMARY KEY,

                                 list_id UUID NOT NULL,
                                 media_id UUID NOT NULL,
                                 position INTEGER NOT NULL,

                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                 CONSTRAINT fk_user_list_items_list
                                     FOREIGN KEY (list_id)
                                         REFERENCES user_lists(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_user_list_items_media
                                     FOREIGN KEY (media_id)
                                         REFERENCES media(id)
                                         ON DELETE RESTRICT,

                                 CONSTRAINT uq_user_list_items_list_media
                                     UNIQUE (list_id, media_id),

                                 CONSTRAINT chk_user_list_items_position
                                     CHECK (position >= 0)
);

CREATE INDEX idx_user_lists_owner_updated
    ON user_lists (owner_id, updated_at DESC, id DESC);

CREATE INDEX idx_user_lists_visibility_updated
    ON user_lists (visibility, updated_at DESC, id DESC);

CREATE INDEX idx_user_list_items_order
    ON user_list_items (list_id, position, id);

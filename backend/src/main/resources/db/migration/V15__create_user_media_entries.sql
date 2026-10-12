
CREATE TABLE user_media_entries (
                                    id UUID PRIMARY KEY,

                                    user_id UUID NOT NULL,
                                    media_id UUID NOT NULL,

                                    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
                                    rating NUMERIC(3,1),

                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                    CONSTRAINT fk_user_media_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES app_users(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_user_media_media
                                        FOREIGN KEY (media_id)
                                            REFERENCES media(id)
                                            ON DELETE RESTRICT,

                                    CONSTRAINT uq_user_media_entries_user_media
                                        UNIQUE (user_id, media_id),

                                    CONSTRAINT chk_user_media_status
                                        CHECK (status IN (
                                                          'PLANNED',
                                                          'IN_PROGRESS',
                                                          'COMPLETED',
                                                          'ON_HOLD',
                                                          'DROPPED'
                                            )),

                                    CONSTRAINT chk_user_media_rating
                                        CHECK (
                                            rating IS NULL
                                                OR rating BETWEEN 1.0 AND 10.0
                                            )
);

CREATE INDEX idx_user_media_entries_user_status
    ON user_media_entries (user_id, status);

CREATE INDEX idx_user_media_entries_user_updated
    ON user_media_entries (user_id, updated_at DESC);

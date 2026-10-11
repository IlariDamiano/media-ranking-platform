
ALTER TABLE media_external_ids
    ADD COLUMN sync_disabled BOOLEAN NOT NULL DEFAULT FALSE;


ALTER TABLE media_external_ids
    ADD COLUMN last_sync_attempt_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN next_sync_attempt_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN sync_failure_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE media_external_ids
    ADD CONSTRAINT chk_sync_failure_count_nonnegative
        CHECK (sync_failure_count >= 0);

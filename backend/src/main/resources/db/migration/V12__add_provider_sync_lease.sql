
ALTER TABLE provider_sync_state
    ADD COLUMN sync_lease_owner UUID,
    ADD COLUMN sync_lease_until TIMESTAMPTZ;

ALTER TABLE provider_sync_state
    ADD CONSTRAINT chk_provider_sync_lease_pair
        CHECK (
            (sync_lease_owner IS NULL AND sync_lease_until IS NULL)
                OR
            (sync_lease_owner IS NOT NULL AND sync_lease_until IS NOT NULL)
            );

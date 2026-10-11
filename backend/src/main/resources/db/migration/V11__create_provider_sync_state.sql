
CREATE TABLE provider_sync_state (
                                     provider VARCHAR(50) PRIMARY KEY,
                                     cooldown_until TIMESTAMP WITH TIME ZONE,
                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

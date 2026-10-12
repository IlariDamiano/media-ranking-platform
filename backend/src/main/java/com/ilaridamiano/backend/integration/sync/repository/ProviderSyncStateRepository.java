
package com.ilaridamiano.backend.integration.sync.repository;

import com.ilaridamiano.backend.integration.sync.entity.ProviderSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProviderSyncStateRepository
        extends JpaRepository<ProviderSyncState, String> {

    @Modifying
    @Query(value = """
        INSERT INTO provider_sync_state (
            provider,
            sync_lease_owner,
            sync_lease_until,
            updated_at
        )
        VALUES (
            :provider,
            :owner,
            :leaseUntil,
            :now
        )
        ON CONFLICT (provider)
        DO UPDATE SET
            sync_lease_owner = EXCLUDED.sync_lease_owner,
            sync_lease_until = EXCLUDED.sync_lease_until,
            updated_at = EXCLUDED.updated_at
        WHERE (
            provider_sync_state.sync_lease_until IS NULL
            OR provider_sync_state.sync_lease_until <= :now
        )
        AND (
            provider_sync_state.cooldown_until IS NULL
            OR provider_sync_state.cooldown_until <= :now
        )
        """, nativeQuery = true)
    int tryAcquireSyncLease(
            @Param("provider") String provider,
            @Param("owner") UUID owner,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("now") Instant now
    );

    @Modifying
    @Query(value = """
        UPDATE provider_sync_state
        SET sync_lease_owner = NULL,
            sync_lease_until = NULL,
            updated_at = :now
        WHERE provider = :provider
          AND sync_lease_owner = :owner
        """, nativeQuery = true)
    int releaseSyncLease(
            @Param("provider") String provider,
            @Param("owner") UUID owner,
            @Param("now") Instant now
    );

}

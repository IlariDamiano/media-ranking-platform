
package com.ilaridamiano.backend.integration.sync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "provider_sync_state")
@Getter
public class ProviderSyncState {

    @Id
    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    @Setter
    @Column(name = "cooldown_until")
    private Instant cooldownUntil;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProviderSyncState() {
    }

    public ProviderSyncState(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider must not be blank"
            );
        }

        this.provider = provider;
        this.updatedAt = Instant.now();
    }
}

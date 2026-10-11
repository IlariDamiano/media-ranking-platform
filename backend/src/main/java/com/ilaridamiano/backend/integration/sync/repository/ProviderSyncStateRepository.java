
package com.ilaridamiano.backend.integration.sync.repository;

import com.ilaridamiano.backend.integration.sync.entity.ProviderSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderSyncStateRepository
        extends JpaRepository<ProviderSyncState, String> {
}


package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SyncTrackingService {

    private final MediaExternalIdRepository externalIdRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID externalIdRecordId) {

        MediaExternalId externalId = externalIdRepository
                .findById(externalIdRecordId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "External ID record not found: " + externalIdRecordId
                ));

        int failureCount = externalId.getSyncFailureCount() + 1;

        Instant now = clock.instant();

        Instant nextRetry = now.plus(
                SyncRetryPolicy.calculateDelay(failureCount)
        );

        externalId.setSyncFailureCount(failureCount);
        externalId.setLastSyncAttemptAt(now);
        externalId.setNextSyncAttemptAt(nextRetry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID externalIdRecordId) {

        MediaExternalId externalId = externalIdRepository
                .findById(externalIdRecordId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "External ID record not found: " + externalIdRecordId
                ));

        Instant now = clock.instant();

        externalId.setLastSyncAttemptAt(now);
        externalId.setNextSyncAttemptAt(null);
        externalId.setSyncFailureCount(0);
    }

}

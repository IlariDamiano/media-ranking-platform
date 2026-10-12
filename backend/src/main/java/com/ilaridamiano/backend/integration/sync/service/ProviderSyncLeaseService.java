
package com.ilaridamiano.backend.integration.sync.service;

import com.ilaridamiano.backend.integration.sync.repository.ProviderSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderSyncLeaseService {

    private static final Duration LEASE_DURATION =
            Duration.ofMinutes(30);

    private final ProviderSyncStateRepository repository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(String provider, UUID owner) {

        validateArguments(provider, owner);

        Instant now = clock.instant();

        return repository.tryAcquireSyncLease(
                provider,
                owner,
                now.plus(LEASE_DURATION),
                now
        ) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean release(String provider, UUID owner) {

        validateArguments(provider, owner);

        return repository.releaseSyncLease(
                provider,
                owner,
                clock.instant()
        ) == 1;
    }

    private static void validateArguments(
            String provider,
            UUID owner
    ) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider must not be blank"
            );
        }

        Objects.requireNonNull(owner, "Lease owner is required");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renew(String provider, UUID owner) {

        validateArguments(provider, owner);

        Instant now = clock.instant();

        return repository.renewSyncLease(
                provider,
                owner,
                now.plus(LEASE_DURATION),
                now
        ) == 1;
    }

}


package com.ilaridamiano.backend.integration.sync.service;

import com.ilaridamiano.backend.integration.sync.repository.ProviderSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ilaridamiano.backend.integration.sync.entity.ProviderSyncState;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ProviderCooldownService {

    private static final Duration DEFAULT_RATE_LIMIT_COOLDOWN =
            Duration.ofMinutes(30);

    private final ProviderSyncStateRepository providerSyncStateRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRateLimit(String provider) {

        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider must not be blank"
            );
        }

        Instant now = clock.instant();

        ProviderSyncState state = providerSyncStateRepository
                .findById(provider)
                .orElseGet(() -> new ProviderSyncState(provider));

        Instant proposedCooldown = now.plus(DEFAULT_RATE_LIMIT_COOLDOWN);

        // Never shorten an existing cooldown.
        if (state.getCooldownUntil() == null
                || state.getCooldownUntil().isBefore(proposedCooldown)) {
            state.setCooldownUntil(proposedCooldown);
        }

        state.setUpdatedAt(now);

        providerSyncStateRepository.save(state);
    }

    @Transactional(readOnly = true)
    public boolean isProviderAvailable(String provider) {

        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "Provider must not be blank"
            );
        }

        Instant now = clock.instant();

        return providerSyncStateRepository
                .findById(provider)
                .map(state -> {
                    Instant cooldownUntil = state.getCooldownUntil();

                    return cooldownUntil == null
                            || !now.isBefore(cooldownUntil);
                })
                .orElse(true);
    }

}

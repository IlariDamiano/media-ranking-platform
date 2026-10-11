
package com.ilaridamiano.backend.integration.sync.service;

import com.ilaridamiano.backend.integration.sync.entity.ProviderSyncState;
import com.ilaridamiano.backend.integration.sync.repository.ProviderSyncStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProviderCooldownServiceIntegrationTest {

    private final ProviderCooldownService cooldownService;
    private final ProviderSyncStateRepository stateRepository;

    @Autowired
    ProviderCooldownServiceIntegrationTest(
            ProviderCooldownService cooldownService,
            ProviderSyncStateRepository stateRepository
    ) {
        this.cooldownService = cooldownService;
        this.stateRepository = stateRepository;
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-10-11T12:00:00Z"),
                    ZoneOffset.UTC
            );
        }
    }

    @Test
    void createsProviderCooldownAfterRateLimit() {

        String provider = "TEST_RATE_LIMIT";

        try {
            cooldownService.recordRateLimit(provider);

            ProviderSyncState state = stateRepository
                    .findById(provider)
                    .orElseThrow();

            assertThat(state.getCooldownUntil())
                    .isEqualTo(
                            Instant.parse("2026-10-11T12:30:00Z")
                    );

            assertThat(state.getUpdatedAt())
                    .isEqualTo(
                            Instant.parse("2026-10-11T12:00:00Z")
                    );

        } finally {
            stateRepository.deleteById(provider);
        }
    }

    @Test
    void preservesExistingLongerCooldown() {

        String provider = "TEST_LONG_COOLDOWN";

        ProviderSyncState state = new ProviderSyncState(provider);

        state.setCooldownUntil(
                Instant.parse("2026-10-11T15:00:00Z")
        );

        stateRepository.saveAndFlush(state);

        try {
            cooldownService.recordRateLimit(provider);

            ProviderSyncState updatedState = stateRepository
                    .findById(provider)
                    .orElseThrow();

            assertThat(updatedState.getCooldownUntil())
                    .isEqualTo(
                            Instant.parse("2026-10-11T15:00:00Z")
                    );

            assertThat(updatedState.getUpdatedAt())
                    .isEqualTo(
                            Instant.parse("2026-10-11T12:00:00Z")
                    );

        } finally {
            stateRepository.deleteById(provider);
        }
    }

    @Test
    void allowsProviderWhenNoCooldownRecordExists() {

        String provider = "TEST_NO_COOLDOWN";

        assertThat(stateRepository.findById(provider))
                .isEmpty();

        assertThat(cooldownService.isProviderAvailable(provider))
                .isTrue();
    }

    @Test
    void blocksProviderDuringActiveCooldown() {

        String provider = "TEST_ACTIVE_COOLDOWN";

        ProviderSyncState state = new ProviderSyncState(provider);

        state.setCooldownUntil(
                Instant.parse("2026-10-11T12:30:00Z")
        );

        stateRepository.saveAndFlush(state);

        try {
            assertThat(cooldownService.isProviderAvailable(provider))
                    .isFalse();

        } finally {
            stateRepository.deleteById(provider);
        }
    }

    @Test
    void allowsProviderAfterCooldownExpires() {

        String provider = "TEST_EXPIRED_COOLDOWN";

        ProviderSyncState state = new ProviderSyncState(provider);

        state.setCooldownUntil(
                Instant.parse("2026-10-11T11:30:00Z")
        );

        stateRepository.saveAndFlush(state);

        try {
            assertThat(cooldownService.isProviderAvailable(provider))
                    .isTrue();

        } finally {
            stateRepository.deleteById(provider);
        }
    }
}

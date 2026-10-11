
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
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
class SyncTrackingServiceIntegrationTest {

    private final SyncTrackingService trackingService;
    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;

    @Autowired
    SyncTrackingServiceIntegrationTest(
            SyncTrackingService trackingService,
            MediaRepository mediaRepository,
            MediaExternalIdRepository externalIdRepository
    ) {
        this.trackingService = trackingService;
        this.mediaRepository = mediaRepository;
        this.externalIdRepository = externalIdRepository;
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
    void recordsFailuresWithIncreasingCooldown() {

        Media movie = mediaRepository.saveAndFlush(
                new Media("Sync Tracking Test Movie", MediaType.MOVIE)
        );

        MediaExternalId externalId = externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        movie, "TMDB", "MOVIE", "990001"
                )
        );

        try {
            trackingService.recordFailure(externalId.getId());

            MediaExternalId afterFirstFailure = externalIdRepository
                    .findById(externalId.getId())
                    .orElseThrow();

            assertThat(afterFirstFailure.getSyncFailureCount())
                    .isEqualTo(1);

            assertThat(afterFirstFailure.getLastSyncAttemptAt())
                    .isEqualTo(Instant.parse("2026-10-11T12:00:00Z"));

            assertThat(afterFirstFailure.getNextSyncAttemptAt())
                    .isEqualTo(Instant.parse("2026-10-11T12:15:00Z"));

            trackingService.recordFailure(externalId.getId());

            MediaExternalId afterSecondFailure = externalIdRepository
                    .findById(externalId.getId())
                    .orElseThrow();

            assertThat(afterSecondFailure.getSyncFailureCount())
                    .isEqualTo(2);

            assertThat(afterSecondFailure.getNextSyncAttemptAt())
                    .isEqualTo(Instant.parse("2026-10-11T12:30:00Z"));

        } finally {
            externalIdRepository.deleteById(externalId.getId());
            mediaRepository.deleteById(movie.getId());
        }
    }

    @Test
    void resetsFailureTrackingAfterSuccessfulSync() {

        Media movie = mediaRepository.saveAndFlush(
                new Media("Sync Success Test Movie", MediaType.MOVIE)
        );

        MediaExternalId externalId = externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        movie, "TMDB", "MOVIE", "990002"
                )
        );

        try {
            // First, simulate two failures.
            trackingService.recordFailure(externalId.getId());
            trackingService.recordFailure(externalId.getId());

            MediaExternalId afterFailures = externalIdRepository
                    .findById(externalId.getId())
                    .orElseThrow();

            assertThat(afterFailures.getSyncFailureCount())
                    .isEqualTo(2);

            assertThat(afterFailures.getNextSyncAttemptAt())
                    .isNotNull();

            // Now simulate a successful synchronization.
            trackingService.recordSuccess(externalId.getId());

            MediaExternalId afterSuccess = externalIdRepository
                    .findById(externalId.getId())
                    .orElseThrow();

            assertThat(afterSuccess.getSyncFailureCount())
                    .isZero();

            assertThat(afterSuccess.getNextSyncAttemptAt())
                    .isNull();

            assertThat(afterSuccess.getLastSyncAttemptAt())
                    .isEqualTo(Instant.parse("2026-10-11T12:00:00Z"));

        } finally {
            externalIdRepository.deleteById(externalId.getId());
            mediaRepository.deleteById(movie.getId());
        }
    }

}

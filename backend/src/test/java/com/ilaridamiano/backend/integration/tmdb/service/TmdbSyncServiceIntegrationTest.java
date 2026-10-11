
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TmdbSyncServiceIntegrationTest {

    private final TmdbSyncService syncService;
    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;

    @Autowired
    TmdbSyncServiceIntegrationTest(
            TmdbSyncService syncService,
            MediaRepository mediaRepository,
            MediaExternalIdRepository externalIdRepository
    ) {
        this.syncService = syncService;
        this.mediaRepository = mediaRepository;
        this.externalIdRepository = externalIdRepository;
    }

    @Test
    void selectsOnlyTheRequestedBatchSize() {

        Instant oldDate = Instant.now().minus(10, ChronoUnit.DAYS);

        for (int i = 1; i <= 3; i++) {
            Media movie = new Media(
                    "Sync Test Movie " + i,
                    MediaType.MOVIE
            );
            movie.setLastSyncedAt(oldDate);

            mediaRepository.saveAndFlush(movie);

            externalIdRepository.saveAndFlush(
                    new MediaExternalId(
                            movie, "TMDB", "MOVIE", "90000" + i
                    )
            );
        }

        Page<Media> batch = syncService.findOutdatedMovies(2);

        assertThat(batch.getContent()).hasSize(2);
        assertThat(batch.getTotalElements()).isEqualTo(3);
        assertThat(batch.hasNext()).isTrue();
    }

    @Test
    void rejectsInvalidBatchSizes() {

        assertThatThrownBy(() -> syncService.findOutdatedMovies(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> syncService.findOutdatedMovies(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void prioritizesNeverSyncedMovies() {

        Instant now = Instant.now();

        Media neverSynced = new Media(
                "Never Synced", MediaType.MOVIE
        );

        Media oldMovie = new Media(
                "Old Synced", MediaType.MOVIE
        );
        oldMovie.setLastSyncedAt(
                now.minus(30, ChronoUnit.DAYS)
        );

        Media recentMovie = new Media(
                "Recently Synced", MediaType.MOVIE
        );
        recentMovie.setLastSyncedAt(
                now.minus(1, ChronoUnit.DAYS)
        );

        mediaRepository.saveAndFlush(neverSynced);
        mediaRepository.saveAndFlush(oldMovie);
        mediaRepository.saveAndFlush(recentMovie);

        externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        neverSynced, "TMDB", "MOVIE", "910001"
                )
        );

        externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        oldMovie, "TMDB", "MOVIE", "910002"
                )
        );

        externalIdRepository.saveAndFlush(
                new MediaExternalId(
                        recentMovie, "TMDB", "MOVIE", "910003"
                )
        );

        Page<Media> batch = syncService.findOutdatedMovies(100);

        assertThat(batch.getContent())
                .extracting(Media::getTitle)
                .containsSubsequence("Never Synced", "Old Synced")
                .doesNotContain("Recently Synced");
    }

    @Test
    void excludesMoviesWithActiveRetryCooldown() {

        Instant now = Instant.parse("2026-10-11T12:00:00Z");
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        Media blockedMovie = new Media(
                "Blocked Retry Test Movie", MediaType.MOVIE
        );

        blockedMovie.setLastSyncedAt(now.minus(10, ChronoUnit.DAYS));
        blockedMovie = mediaRepository.saveAndFlush(blockedMovie);

        MediaExternalId blockedExternalId = new MediaExternalId(
                blockedMovie, "TMDB", "MOVIE", "990101"
        );

        blockedExternalId.setNextSyncAttemptAt(
                now.plus(30, ChronoUnit.MINUTES)
        );

        externalIdRepository.saveAndFlush(blockedExternalId);

        Media eligibleMovie = new Media(
                "Eligible Retry Test Movie", MediaType.MOVIE
        );

        eligibleMovie.setLastSyncedAt(now.minus(10, ChronoUnit.DAYS));
        eligibleMovie = mediaRepository.saveAndFlush(eligibleMovie);

        MediaExternalId eligibleExternalId = new MediaExternalId(
                eligibleMovie, "TMDB", "MOVIE", "990102"
        );

        eligibleExternalId.setNextSyncAttemptAt(
                now.minus(5, ChronoUnit.MINUTES)
        );

        externalIdRepository.saveAndFlush(eligibleExternalId);

        Page<Media> results = mediaRepository.findMediaNeedingSync(
                MediaType.MOVIE,
                cutoff,
                now,
                PageRequest.of(0, 100)
        );

        assertThat(results.getContent())
                .extracting(Media::getId)
                .contains(eligibleMovie.getId())
                .doesNotContain(blockedMovie.getId());
    }

}

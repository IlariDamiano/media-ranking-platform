
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import com.ilaridamiano.backend.integration.tmdb.client.TmdbClient;
import com.ilaridamiano.backend.integration.tmdb.mapper.TmdbMovieMapper;
import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TmdbImportService {

    private final TmdbClient tmdbClient;
    private final TmdbMovieMapper tmdbMovieMapper;
    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;


    public TmdbImportService(
            TmdbClient tmdbClient,
            TmdbMovieMapper tmdbMovieMapper,
            MediaRepository mediaRepository,
            MediaExternalIdRepository externalIdRepository
    ) {
        this.tmdbClient = tmdbClient;
        this.tmdbMovieMapper = tmdbMovieMapper;
        this.mediaRepository = mediaRepository;
        this.externalIdRepository = externalIdRepository;
    }


    @Transactional
    public Media importMovie(Long tmdbId) {

        String externalId = tmdbId.toString();

        Optional<MediaExternalId> existing =
                externalIdRepository
                        .findByProviderAndProviderMediaTypeAndExternalId(
                                "TMDB",
                                "MOVIE",
                                externalId
                        );

        if (existing.isPresent()) {
            return existing.get().getMedia();
        }

        TmdbMovieResponse response = tmdbClient.getMovie(tmdbId);

        Media movie = tmdbMovieMapper.toMedia(response);
        Media savedMovie = mediaRepository.save(movie);
        movie.setLastSyncedAt(Instant.now());

        MediaExternalId externalIdRecord = new MediaExternalId(
                savedMovie,
                "TMDB",
                "MOVIE",
                externalId
        );

        externalIdRepository.save(externalIdRecord);

        return savedMovie;
    }

    @Transactional
    public Media refreshMovie(Long tmdbId) {

        String externalId = tmdbId.toString();

        MediaExternalId existingExternalId = externalIdRepository
                .findByProviderAndProviderMediaTypeAndExternalId(
                        "TMDB",
                        "MOVIE",
                        externalId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Movie not imported yet: TMDB ID " + tmdbId
                ));

        Media existingMovie = existingExternalId.getMedia();

        TmdbMovieResponse response = tmdbClient.getMovie(tmdbId);

        tmdbMovieMapper.updateDiscoveryMetadata(
                response,
                existingMovie
        );

        existingMovie.setLastSyncedAt(Instant.now());

        return existingMovie;
    }


}

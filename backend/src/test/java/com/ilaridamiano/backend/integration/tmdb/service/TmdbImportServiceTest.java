
package com.ilaridamiano.backend.integration.tmdb.service;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.entity.MediaExternalId;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.catalog.repository.MediaRepository;
import com.ilaridamiano.backend.catalog.repository.MediaExternalIdRepository;
import com.ilaridamiano.backend.integration.tmdb.client.TmdbClient;
import com.ilaridamiano.backend.integration.tmdb.mapper.TmdbMovieMapper;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TmdbImportServiceTest {

    @Test
    void returnsExistingMovieWithoutCallingTmdb() {

        TmdbClient tmdbClient = mock(TmdbClient.class);
        TmdbMovieMapper mapper = mock(TmdbMovieMapper.class);
        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);

        Media movie = new Media("Interstellar", MediaType.MOVIE);

        MediaExternalId externalId = new MediaExternalId(
                movie,
                "TMDB",
                "MOVIE",
                "157336"
        );

        when(externalIdRepository.findByProviderAndProviderMediaTypeAndExternalId(
                "TMDB", "MOVIE", "157336"
        )).thenReturn(Optional.of(externalId));

        TmdbImportService service = new TmdbImportService(
                tmdbClient,
                mapper,
                mediaRepository,
                externalIdRepository
        );

        Media result = service.importMovie(157336L);

        assertThat(result).isSameAs(movie);

        verifyNoInteractions(tmdbClient, mapper, mediaRepository);
    }


    @Test
    void importsNewMovieWhenNotAlreadyPresent() {

        TmdbClient tmdbClient = mock(TmdbClient.class);
        TmdbMovieMapper mapper = mock(TmdbMovieMapper.class);
        MediaRepository mediaRepository = mock(MediaRepository.class);
        MediaExternalIdRepository externalIdRepository =
                mock(MediaExternalIdRepository.class);

        TmdbMovieResponse response = mock(TmdbMovieResponse.class);
        Media movie = new Media("Interstellar", MediaType.MOVIE);

        when(externalIdRepository
                .findByProviderAndProviderMediaTypeAndExternalId(
                        "TMDB", "MOVIE", "157336"
                )).thenReturn(Optional.empty());

        when(tmdbClient.getMovie(157336L)).thenReturn(response);
        when(mapper.toMedia(response)).thenReturn(movie);
        when(mediaRepository.save(movie)).thenReturn(movie);

        TmdbImportService service = new TmdbImportService(
                tmdbClient,
                mapper,
                mediaRepository,
                externalIdRepository
        );

        Media result = service.importMovie(157336L);

        assertThat(result).isSameAs(movie);

        verify(tmdbClient).getMovie(157336L);
        verify(mapper).toMedia(response);
        verify(mediaRepository).save(movie);

        verify(externalIdRepository).save(argThat(externalId ->
                externalId.getMedia() == movie
                        && externalId.getProvider().equals("TMDB")
                        && externalId.getProviderMediaType().equals("MOVIE")
                        && externalId.getExternalId().equals("157336")
        ));
    }

}

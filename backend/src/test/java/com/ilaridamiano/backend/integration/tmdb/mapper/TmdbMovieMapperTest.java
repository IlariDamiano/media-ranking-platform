
package com.ilaridamiano.backend.integration.tmdb.mapper;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class TmdbMovieMapperTest {

    @Test
    void canMapTmdbMovieToMedia() {

        // Arrange: simulate the response from TMDB
        TmdbMovieResponse response = new TmdbMovieResponse(
                157336L,
                "Interstellar",
                "Interstellar",
                "A group of explorers travels through a wormhole.",
                "2014-11-04",
                169,
                "en",
                "/poster.jpg",
                125.8,
                8.5,
                38000
        );

        TmdbMovieMapper mapper = new TmdbMovieMapper();

        // Act: convert the response
        Media media = mapper.toMedia(response);

        // Assert: verify the converted fields
        assertThat(media.getTitle()).isEqualTo("Interstellar");
        assertThat(media.getMediaType()).isEqualTo(MediaType.MOVIE);
        assertThat(media.getDescription())
                .isEqualTo("A group of explorers travels through a wormhole.");

        assertThat(media.getReleaseDate())
                .isEqualTo(LocalDate.of(2014, 11, 4));

        assertThat(media.getId()).isNull();

        assertThat(media.getPopularityScore())
                .isEqualTo(125.8);

        assertThat(media.getExternalRating())
                .isEqualTo(8.5);

        assertThat(media.getExternalRatingCount())
                .isEqualTo(38000);
        assertThat(media.getPosterPath())
                .isEqualTo("/poster.jpg");

    }
}

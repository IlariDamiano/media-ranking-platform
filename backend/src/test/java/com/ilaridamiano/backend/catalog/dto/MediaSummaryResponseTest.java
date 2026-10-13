
package com.ilaridamiano.backend.catalog.dto;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaSummaryResponseTest {

    @Test
    void buildsPosterUrlWhenArtworkExists() {
        Media movie = new Media("Interstellar", MediaType.MOVIE);
        movie.setPosterPath("/poster.jpg");

        MediaSummaryResponse response =
                MediaSummaryResponse.fromMedia(movie);

        assertThat(response.posterUrl())
                .isEqualTo(
                        "https://image.tmdb.org/t/p/w500/poster.jpg"
                );
    }

    @Test
    void returnsNullWhenArtworkIsMissing() {
        Media movie = new Media("Interstellar", MediaType.MOVIE);

        MediaSummaryResponse response =
                MediaSummaryResponse.fromMedia(movie);

        assertThat(response.posterUrl()).isNull();
    }
}

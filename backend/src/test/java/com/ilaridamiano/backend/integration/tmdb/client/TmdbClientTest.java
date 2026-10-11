package com.ilaridamiano.backend.integration.tmdb.client;

import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;

class TmdbClientTest {

    @Test
    @Tag("external")
    @EnabledIfEnvironmentVariable(
            named = "TMDB_ACCESS_TOKEN",
            matches = ".+"
    )
    void canFetchMovieFromTmdb() {

        // Arrange
        String baseUrl = "https://api.themoviedb.org/3";
        String accessToken = System.getenv("TMDB_ACCESS_TOKEN");

        assertThat(accessToken)
                .as("TMDB_ACCESS_TOKEN must be configured")
                .isNotBlank();

        TmdbClient client = new TmdbClient(baseUrl, accessToken);

        // Act
        TmdbMovieResponse movie = client.getMovie(157336L);

        // Assert
        assertThat(movie).isNotNull();
        assertThat(movie.id()).isEqualTo(157336L);
        assertThat(movie.title()).isEqualTo("Interstellar");
        assertThat(movie.originalLanguage()).isEqualTo("en");

        // Display some of the retrieved information
        System.out.println("Title: " + movie.title());
        System.out.println("Original title: " + movie.originalTitle());
        System.out.println("Release date: " + movie.releaseDate());
        System.out.println("Runtime: " + movie.runtime() + " minutes");
        System.out.println("Poster path: " + movie.posterPath());
    }
}
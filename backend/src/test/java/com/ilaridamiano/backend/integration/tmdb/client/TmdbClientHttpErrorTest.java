
package com.ilaridamiano.backend.integration.tmdb.client;

import com.ilaridamiano.backend.integration.tmdb.exception.TmdbPermanentException;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbRateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TmdbClientHttpErrorTest {

    @Test
    void throwsPermanentExceptionWhenMovieIsNotFound() {

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3");

        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder)
                .build();

        TmdbClient client = new TmdbClient(builder.build());

        server.expect(requestTo(
                        "https://api.themoviedb.org/3/movie/999999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getMovie(999999L))
                .isInstanceOf(TmdbPermanentException.class)
                .hasMessageContaining("999999");

        server.verify();
    }

    @Test
    void throwsRateLimitExceptionWhenTmdbReturns429() {

        // Arrange: create an HTTP client pointing to TMDB.
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3");

        // Intercept requests without contacting the real API.
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder)
                .build();

        TmdbClient client = new TmdbClient(builder.build());

        // Simulate TMDB rejecting a request due to rate limiting.
        server.expect(requestTo(
                        "https://api.themoviedb.org/3/movie/157336"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        // Act and assert: our custom exception must be thrown.
        assertThatThrownBy(() -> client.getMovie(157336L))
                .isInstanceOf(TmdbRateLimitException.class)
                .hasMessageContaining("rate limit");

        // Ensure the expected HTTP request occurred.
        server.verify();
    }

}

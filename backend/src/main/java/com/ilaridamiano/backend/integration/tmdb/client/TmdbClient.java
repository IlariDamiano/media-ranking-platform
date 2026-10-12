package com.ilaridamiano.backend.integration.tmdb.client;

import com.ilaridamiano.backend.integration.tmdb.exception.TmdbRateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbPermanentException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import com.ilaridamiano.backend.integration.tmdb.exception.TmdbAuthenticationException;

@Component
public class TmdbClient {

    private final RestClient restClient;

    @Autowired
    public TmdbClient(
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.access-token}") String accessToken
    ) {
        this(RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build());
    }

    public TmdbClient(RestClient restClient) {
        this.restClient = restClient;
    }


    public TmdbMovieResponse getMovie(Long movieId) {

        return restClient.get()
                .uri("/movie/{movieId}", movieId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 401,
                        (request, response) -> {
                            throw new TmdbAuthenticationException(
                                    "TMDB authentication failed (HTTP 401)"
                            );
                        }
                )
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new TmdbPermanentException(
                                    "TMDB movie not found: " + movieId
                            );
                        }
                )
                .onStatus(
                        status -> status.value() == 429,
                        (request, response) -> {
                            throw new TmdbRateLimitException(
                                    "TMDB rate limit exceeded"
                            );
                        }
                )
                .body(TmdbMovieResponse.class);
    }

}
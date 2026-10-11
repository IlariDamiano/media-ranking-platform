package com.ilaridamiano.backend.integration.tmdb.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;

@Component
public class TmdbClient {

    private final RestClient restClient;

    public TmdbClient(
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.access-token}") String accessToken
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    public TmdbMovieResponse getMovie(Long movieId) {
        return restClient.get()
                .uri("/movie/{movieId}", movieId)
                .retrieve()
                .body(TmdbMovieResponse.class);
    }
}
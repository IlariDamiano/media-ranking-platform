package com.ilaridamiano.backend.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieResponse(

        Long id,

        String title,

        @JsonProperty("original_title")
        String originalTitle,

        String overview,

        @JsonProperty("release_date")
        String releaseDate,

        Integer runtime,

        @JsonProperty("original_language")
        String originalLanguage,

        @JsonProperty("poster_path")
        String posterPath,

        @JsonProperty("popularity")
        Double popularity,

        @JsonProperty("vote_average")
        Double voteAverage,

        @JsonProperty("vote_count")
        Integer voteCount


) {
}
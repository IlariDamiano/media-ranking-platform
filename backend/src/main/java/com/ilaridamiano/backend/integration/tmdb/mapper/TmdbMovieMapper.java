package com.ilaridamiano.backend.integration.tmdb.mapper;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.integration.tmdb.dto.TmdbMovieResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TmdbMovieMapper {

    public Media toMedia(TmdbMovieResponse response) {

        Media media = new Media(response.title(), MediaType.MOVIE);

        updateDiscoveryMetadata(response, media);

        media.setDescription(response.overview());

        if (response.releaseDate() != null && !response.releaseDate().isBlank()) {
            media.setReleaseDate(LocalDate.parse(response.releaseDate()));
        }

        return media;
    }

    public void updateDiscoveryMetadata(
            TmdbMovieResponse response,
            Media media
    ) {
        media.setPopularityScore(response.popularity());
        media.setExternalRating(response.voteAverage());
        media.setExternalRatingCount(response.voteCount());
        media.setPosterPath(response.posterPath());
    }

}


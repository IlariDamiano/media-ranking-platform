
package com.ilaridamiano.backend.catalog.dto;

import com.ilaridamiano.backend.catalog.entity.Media;
import com.ilaridamiano.backend.catalog.enums.MediaType;

import java.util.UUID;

public record MediaSummaryResponse(
        UUID id,
        String title,
        MediaType mediaType,
        String posterUrl
) {
    private static final String TMDB_IMAGE_BASE_URL =
            "https://image.tmdb.org/t/p/w500";

    public static MediaSummaryResponse fromMedia(Media media) {
        String path = media.getPosterPath();

        String posterUrl = path != null
                && !path.isBlank()
                && path.startsWith("/")
                ? TMDB_IMAGE_BASE_URL + path
                : null;

        return new MediaSummaryResponse(
                media.getId(),
                media.getTitle(),
                media.getMediaType(),
                posterUrl
        );
    }
}

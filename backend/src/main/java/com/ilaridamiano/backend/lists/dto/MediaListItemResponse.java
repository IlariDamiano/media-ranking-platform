package com.ilaridamiano.backend.lists.dto;

import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.lists.entity.MediaListItem;

import java.util.UUID;

public record MediaListItemResponse(
        UUID id,
        UUID mediaId,
        String title,
        MediaType mediaType,
        int position
) {

    public static MediaListItemResponse from(MediaListItem item) {
        return new MediaListItemResponse(
                item.getId(),
                item.getMedia().getId(),
                item.getMedia().getTitle(),
                item.getMedia().getMediaType(),
                item.getPosition()
        );
    }
}
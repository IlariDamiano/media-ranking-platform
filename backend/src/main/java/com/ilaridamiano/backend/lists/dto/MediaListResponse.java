package com.ilaridamiano.backend.lists.dto;

import com.ilaridamiano.backend.lists.entity.ListVisibility;
import com.ilaridamiano.backend.lists.entity.MediaList;

import java.time.Instant;
import java.util.UUID;

public record MediaListResponse(
        UUID id,
        String name,
        String description,
        ListVisibility visibility,
        String ownerUsername,
        Instant createdAt,
        Instant updatedAt
) {

    public static MediaListResponse from(MediaList list) {
        return new MediaListResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getVisibility(),
                list.getOwner().getUsername(),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }
}
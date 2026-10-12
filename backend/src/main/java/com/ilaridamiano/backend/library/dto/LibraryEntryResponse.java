
package com.ilaridamiano.backend.library.dto;

import com.ilaridamiano.backend.catalog.enums.MediaType;
import com.ilaridamiano.backend.library.entity.MediaProgressStatus;
import com.ilaridamiano.backend.library.entity.UserMediaEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LibraryEntryResponse(
        UUID id,
        UUID mediaId,
        String title,
        MediaType mediaType,
        MediaProgressStatus status,
        BigDecimal rating,
        Instant createdAt,
        Instant updatedAt
) {

    public static LibraryEntryResponse from(UserMediaEntry entry) {

        return new LibraryEntryResponse(
                entry.getId(),
                entry.getMedia().getId(),
                entry.getMedia().getTitle(),
                entry.getMedia().getMediaType(),
                entry.getStatus(),
                entry.getRating(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}

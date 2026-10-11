package com.ilaridamiano.backend.catalog.dto;

import com.ilaridamiano.backend.catalog.enums.MediaType;

import java.util.UUID;

public record MediaSummaryResponse(
        UUID id,
        String title,
        MediaType mediaType
) {
}
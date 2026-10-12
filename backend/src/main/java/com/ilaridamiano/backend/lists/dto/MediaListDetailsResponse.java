package com.ilaridamiano.backend.lists.dto;

import java.util.List;

public record MediaListDetailsResponse(
        MediaListResponse list,
        List<MediaListItemResponse> items
) {
}
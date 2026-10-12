package com.ilaridamiano.backend.lists.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMediaListItemRequest(
        @NotNull UUID mediaId
) {
}
package com.ilaridamiano.backend.lists.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderMediaListRequest(
        @NotNull
        List<@NotNull UUID> mediaIds
) {
}
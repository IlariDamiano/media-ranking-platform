package com.ilaridamiano.backend.lists.dto;

import com.ilaridamiano.backend.lists.entity.ListVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMediaListRequest(
        @NotBlank @Size(max = 120)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        ListVisibility visibility
) {
}
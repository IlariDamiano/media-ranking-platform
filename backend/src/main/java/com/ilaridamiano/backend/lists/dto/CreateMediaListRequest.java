package com.ilaridamiano.backend.lists.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMediaListRequest(
        @NotBlank @Size(max = 120)
        String name,

        @Size(max = 2000)
        String description
) {
}
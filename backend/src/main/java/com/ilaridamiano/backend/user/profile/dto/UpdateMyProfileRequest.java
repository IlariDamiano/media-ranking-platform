
package com.ilaridamiano.backend.user.profile.dto;

import com.ilaridamiano.backend.user.entity.ProfileVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 500)
        String bio,

        @NotNull
        ProfileVisibility visibility
) {
}

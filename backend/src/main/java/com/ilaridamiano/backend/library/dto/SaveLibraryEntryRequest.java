
package com.ilaridamiano.backend.library.dto;

import com.ilaridamiano.backend.library.entity.MediaProgressStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaveLibraryEntryRequest(

        @NotNull
        MediaProgressStatus status,

        @DecimalMin("1.0")
        @DecimalMax("10.0")
        @Digits(integer = 2, fraction = 1)
        BigDecimal rating

) {
}

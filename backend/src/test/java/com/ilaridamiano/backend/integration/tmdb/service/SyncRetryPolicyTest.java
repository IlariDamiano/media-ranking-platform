
package com.ilaridamiano.backend.integration.tmdb.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncRetryPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "1, 15",
            "2, 30",
            "3, 60",
            "4, 120",
            "5, 240",
            "6, 360",
            "7, 360",
            "20, 360"
    })
    void calculatesExpectedRetryDelay(
            int failureCount,
            long expectedMinutes
    ) {
        Duration result =
                SyncRetryPolicy.calculateDelay(failureCount);

        assertThat(result)
                .isEqualTo(Duration.ofMinutes(expectedMinutes));
    }

    @Test
    void rejectsInvalidFailureCounts() {
        assertThatThrownBy(
                () -> SyncRetryPolicy.calculateDelay(0)
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
                () -> SyncRetryPolicy.calculateDelay(-1)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}

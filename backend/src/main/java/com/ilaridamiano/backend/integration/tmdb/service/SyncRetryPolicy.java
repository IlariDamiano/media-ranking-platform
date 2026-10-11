
package com.ilaridamiano.backend.integration.tmdb.service;

import java.time.Duration;

public final class SyncRetryPolicy {

    private static final long BASE_DELAY_MINUTES = 15;
    private static final long MAX_DELAY_MINUTES = 360;

    private SyncRetryPolicy() {
    }

    public static Duration calculateDelay(int failureCount) {

        if (failureCount < 1) {
            throw new IllegalArgumentException(
                    "Failure count must be at least 1"
            );
        }

        long delay = BASE_DELAY_MINUTES;

        for (int i = 1; i < failureCount; i++) {
            delay = Math.min(delay * 2, MAX_DELAY_MINUTES);
        }

        return Duration.ofMinutes(delay);
    }
}

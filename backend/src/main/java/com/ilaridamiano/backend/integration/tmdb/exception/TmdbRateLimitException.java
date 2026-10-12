
package com.ilaridamiano.backend.integration.tmdb.exception;

public class TmdbRateLimitException extends RuntimeException {

    public TmdbRateLimitException(String message) {
        super(message);
    }
}

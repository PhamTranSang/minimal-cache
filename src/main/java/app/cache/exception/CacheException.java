package app.cache.exception;

// Base type for errors caused by how the cache is configured or called.
// Internal invariant violations are reported as IllegalStateException instead.
public abstract class CacheException extends RuntimeException {

    protected CacheException(final String message) {
        super(message);
    }

    protected CacheException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

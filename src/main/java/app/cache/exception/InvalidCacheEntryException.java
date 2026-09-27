package app.cache.exception;

public final class InvalidCacheEntryException extends RuntimeException {

    public InvalidCacheEntryException(final String message) {
        super(message);
    }
}

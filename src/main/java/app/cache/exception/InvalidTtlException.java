package app.cache.exception;

public final class InvalidTtlException extends CacheException {

    public InvalidTtlException(final String message) {
        super(message);
    }

    public InvalidTtlException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

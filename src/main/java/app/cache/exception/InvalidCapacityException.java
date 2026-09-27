package app.cache.exception;

public final class InvalidCapacityException extends RuntimeException {

    public InvalidCapacityException(final int capacity) {
        super("capacity must be greater than 0: " + capacity);
    }
}

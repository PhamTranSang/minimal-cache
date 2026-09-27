package app.cache.exception;

public final class MissingEvictionVictimException extends RuntimeException {

    public MissingEvictionVictimException() {
        super("eviction policy returned no victim while cache is full");
    }
}

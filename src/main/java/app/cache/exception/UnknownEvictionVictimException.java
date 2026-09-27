package app.cache.exception;

public final class UnknownEvictionVictimException extends RuntimeException {

    public UnknownEvictionVictimException(final Object key) {
        super("eviction policy returned an unknown key: " + key);
    }
}

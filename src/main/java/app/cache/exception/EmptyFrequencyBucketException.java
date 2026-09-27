package app.cache.exception;

public final class EmptyFrequencyBucketException extends RuntimeException {

    public EmptyFrequencyBucketException(final int frequency) {
        super("Frequency bucket is empty for frequency=" + frequency);
    }
}

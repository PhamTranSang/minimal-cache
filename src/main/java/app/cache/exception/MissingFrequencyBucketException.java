package app.cache.exception;

public final class MissingFrequencyBucketException extends RuntimeException {

    public MissingFrequencyBucketException(final int frequency) {
        super("No frequency bucket found for frequency=" + frequency);
    }
}

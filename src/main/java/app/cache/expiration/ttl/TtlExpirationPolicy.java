package app.cache.expiration.ttl;

import app.cache.expiration.ExpirationPolicy;
import app.cache.exception.InvalidCacheEntryException;
import app.cache.exception.InvalidTtlException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class TtlExpirationPolicy<K> implements ExpirationPolicy<K> {

    private final Map<K, Long> writeTimes = new HashMap<>();
    private final long ttlNanos;

    public TtlExpirationPolicy(final Duration ttl) {
        if (ttl == null) {
            throw new InvalidTtlException("ttl must not be null");
        }

        if (ttl.isZero() || ttl.isNegative()) {
            throw new InvalidTtlException("ttl must be greater than 0");
        }

        try {
            this.ttlNanos = ttl.toNanos();
        } catch (ArithmeticException cause) {
            throw new InvalidTtlException("ttl is too large to represent in nanoseconds", cause);
        }
    }

    @Override
    public void onPut(final K key) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }

        writeTimes.put(key, System.nanoTime());
    }

    @Override
    public boolean isExpired(final K key) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }

        final Long writeTime = writeTimes.get(key);

        if (writeTime == null) {
            return false;
        }

        final long elapsed = System.nanoTime() - writeTime;

        return elapsed >= ttlNanos;
    }

    @Override
    public void onRemove(final K key) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }

        writeTimes.remove(key);
    }

    @Override
    public void clear() {
        writeTimes.clear();
    }
}

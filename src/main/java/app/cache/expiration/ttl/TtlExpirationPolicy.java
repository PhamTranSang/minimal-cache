package app.cache.expiration.ttl;

import app.cache.Ticker;
import app.cache.entry.CacheEntry;
import app.cache.expiration.ExpirationPolicy;
import app.cache.exception.InvalidCacheConfigurationException;
import app.cache.exception.InvalidTtlException;
import java.time.Duration;

public final class TtlExpirationPolicy<K, V> implements ExpirationPolicy<K, V> {

    private final long ttlNanos;
    private final Ticker ticker;

    public TtlExpirationPolicy(final Duration ttl, final Ticker ticker) {
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

        if (ticker == null) {
            throw new InvalidCacheConfigurationException("ticker must not be null");
        }

        this.ticker = ticker;
    }

    @Override
    public void onWrite(final CacheEntry<K, V> entry) {
        entry.setWriteTime(ticker.read());
    }

    @Override
    public boolean isExpired(final CacheEntry<K, V> entry) {
        final long elapsed = ticker.read() - entry.getWriteTime();

        return elapsed >= ttlNanos;
    }
}

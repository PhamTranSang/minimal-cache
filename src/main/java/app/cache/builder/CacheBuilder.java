package app.cache.builder;

import app.cache.Cache;
import app.cache.InMemoryCache;
import app.cache.config.CacheConfig;
import app.cache.eviction.EvictionPolicy;
import app.cache.exception.IncompleteCacheConfigurationException;
import app.cache.expiration.ExpirationPolicy;
import app.cache.expiration.ttl.NoExpirationPolicy;

public final class CacheBuilder<K, V> {

    private Integer capacity;
    private EvictionPolicy<K> evictionPolicy;

    private ExpirationPolicy<K> expirationPolicy = new NoExpirationPolicy<>();

    private CacheBuilder() {
    }

    public static <K, V> CacheBuilder<K, V> newBuilder() {
        return new CacheBuilder<>();
    }

    public CacheBuilder<K, V> capacity(final int capacity) {
        this.capacity = capacity;
        return this;
    }

    public CacheBuilder<K, V> evictionPolicy(final EvictionPolicy<K> evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
        return this;
    }

    public CacheBuilder<K, V> expirationPolicy(final ExpirationPolicy<K> expirationPolicy) {
        this.expirationPolicy = expirationPolicy;
        return this;
    }

    public Cache<K, V> build() {
        if (capacity == null) {
            throw new IncompleteCacheConfigurationException("capacity must be configured");
        }

        if (evictionPolicy == null) {
            throw new IncompleteCacheConfigurationException("evictionPolicy must be configured");
        }

        final CacheConfig<K> config = new CacheConfig<>(capacity, evictionPolicy, expirationPolicy);

        return new InMemoryCache<>(config);
    }
}

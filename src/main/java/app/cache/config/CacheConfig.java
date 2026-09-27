package app.cache.config;

import app.cache.eviction.EvictionPolicy;
import app.cache.exception.InvalidCapacityException;
import app.cache.exception.InvalidCacheConfigurationException;
import app.cache.expiration.ExpirationPolicy;

public record CacheConfig<K>(
    int capacity,
    EvictionPolicy<K> evictionPolicy,
    ExpirationPolicy<K> expirationPolicy
) {

    public CacheConfig {
        if (capacity <= 0) {
            throw new InvalidCapacityException(capacity);
        }

        if (evictionPolicy == null) {
            throw new InvalidCacheConfigurationException("evictionPolicy must not be null");
        }
        if (expirationPolicy == null) {
            throw new InvalidCacheConfigurationException("expirationPolicy must not be null");
        }
    }
}

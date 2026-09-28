package app.cache.config;

import app.cache.eviction.EvictionPolicy;
import app.cache.exception.InvalidCapacityException;
import app.cache.expiration.ExpirationPolicy;

public record CacheConfig<K, V>(int capacity, EvictionPolicy<K, V> evictionPolicy, ExpirationPolicy<K, V> expirationPolicy) {

    public CacheConfig {
        if (capacity <= 0) {
            throw new InvalidCapacityException("capacity must be greater than 0: " + capacity);
        }
    }
}

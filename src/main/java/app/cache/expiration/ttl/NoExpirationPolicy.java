package app.cache.expiration.ttl;

import app.cache.entry.CacheEntry;
import app.cache.expiration.ExpirationPolicy;

public final class NoExpirationPolicy<K, V> implements ExpirationPolicy<K, V> {

    @Override
    public void onWrite(final CacheEntry<K, V> entry) {
    }

    @Override
    public boolean isExpired(final CacheEntry<K, V> entry) {
        return false;
    }
}

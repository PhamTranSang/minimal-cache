package app.cache.expiration.ttl;

import app.cache.expiration.ExpirationPolicy;

public final class NoExpirationPolicy<K> implements ExpirationPolicy<K> {

    @Override
    public void onPut(final K key) {
    }

    @Override
    public boolean isExpired(final K key) {
        return false;
    }

    @Override
    public void onRemove(final K key) {
    }

    @Override
    public void clear() {
    }
}

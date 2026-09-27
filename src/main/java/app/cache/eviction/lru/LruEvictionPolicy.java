package app.cache.eviction.lru;

import app.cache.eviction.EvictionPolicy;

public final class LruEvictionPolicy<K> implements EvictionPolicy<K> {

    private final LruAccessOrder<K> accessOrder = new LruAccessOrder<>();

    @Override
    public void onGet(final K key) {
        accessOrder.access(key);
    }

    @Override
    public void onPut(final K key) {
        accessOrder.add(key);
    }

    @Override
    public void onRemove(final K key) {
        accessOrder.remove(key);
    }

    @Override
    public K evict() {
        return accessOrder.removeLeastRecentlyUsed();
    }

    @Override
    public void clear() {
        accessOrder.clear();
    }
}

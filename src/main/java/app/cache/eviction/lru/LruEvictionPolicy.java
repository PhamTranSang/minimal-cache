package app.cache.eviction.lru;

import app.cache.entry.CacheEntry;
import app.cache.entry.EntryList;
import app.cache.eviction.EvictionPolicy;

public final class LruEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    // Head is the least recently used entry, tail the most recently used.
    private final EntryList<K, V> accessOrder = new EntryList<>();

    @Override
    public void onAdd(final CacheEntry<K, V> entry) {
        accessOrder.addLast(entry);
    }

    @Override
    public void onAccess(final CacheEntry<K, V> entry) {
        accessOrder.moveToLast(entry);
    }

    @Override
    public void onRemove(final CacheEntry<K, V> entry) {
        accessOrder.remove(entry);
    }

    @Override
    public CacheEntry<K, V> evict() {
        return accessOrder.removeFirst();
    }

    @Override
    public void clear() {
        accessOrder.clear();
    }
}

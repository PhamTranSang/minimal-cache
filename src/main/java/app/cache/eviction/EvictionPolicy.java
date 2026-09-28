package app.cache.eviction;

import app.cache.entry.CacheEntry;
import app.cache.eviction.fifo.FifoEvictionPolicy;
import app.cache.eviction.lfu.LfuEvictionPolicy;
import app.cache.eviction.lru.LruEvictionPolicy;

public sealed interface EvictionPolicy<K, V> permits FifoEvictionPolicy, LfuEvictionPolicy, LruEvictionPolicy {

    void onAdd(CacheEntry<K, V> entry);

    default void onAccess(CacheEntry<K, V> entry) {
    }

    void onRemove(CacheEntry<K, V> entry);

    CacheEntry<K, V> evict();

    void clear();
}

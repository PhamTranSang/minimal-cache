package app.cache.eviction;

import app.cache.eviction.fifo.FifoEvictionPolicy;
import app.cache.eviction.lfu.LfuEvictionPolicy;
import app.cache.eviction.lru.LruEvictionPolicy;

public sealed interface EvictionPolicy<K> permits FifoEvictionPolicy, LfuEvictionPolicy, LruEvictionPolicy {

    default void onGet(K key) {
    }

    void onPut(K key);

    void onRemove(K key);

    K evict();

    void clear();
}

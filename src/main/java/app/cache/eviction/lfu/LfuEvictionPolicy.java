package app.cache.eviction.lfu;

import app.cache.entry.CacheEntry;
import app.cache.eviction.EvictionPolicy;

public final class LfuEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    private final LfuFrequencyStructure<K, V> structure = new LfuFrequencyStructure<>();

    @Override
    public void onAdd(final CacheEntry<K, V> entry) {
        structure.add(entry);
    }

    @Override
    public void onAccess(final CacheEntry<K, V> entry) {
        structure.access(entry);
    }

    @Override
    public void onRemove(final CacheEntry<K, V> entry) {
        structure.remove(entry);
    }

    @Override
    public CacheEntry<K, V> evict() {
        return structure.removeLeastFrequentlyUsed();
    }

    @Override
    public void clear() {
        structure.clear();
    }
}

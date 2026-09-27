package app.cache.eviction.lfu;

import app.cache.eviction.EvictionPolicy;

public final class LfuEvictionPolicy<K> implements EvictionPolicy<K> {

    private final LfuFrequencyStructure<K> structure = new LfuFrequencyStructure<>();

    @Override
    public void onGet(final K key) {
        structure.access(key);
    }

    @Override
    public void onPut(final K key) {
        structure.add(key);
    }

    @Override
    public void onRemove(final K key) {
        structure.remove(key);
    }

    @Override
    public K evict() {
        return structure.removeLeastFrequentlyUsed();
    }

    @Override
    public void clear() {
        structure.clear();
    }
}

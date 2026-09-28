package app.cache.eviction.fifo;

import app.cache.entry.CacheEntry;
import app.cache.entry.EntryList;
import app.cache.eviction.EvictionPolicy;

public final class FifoEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    private final EntryList<K, V> insertionOrder = new EntryList<>();

    @Override
    public void onAdd(final CacheEntry<K, V> entry) {
        insertionOrder.addLast(entry);
    }

    @Override
    public void onRemove(final CacheEntry<K, V> entry) {
        insertionOrder.remove(entry);
    }

    @Override
    public CacheEntry<K, V> evict() {
        return insertionOrder.removeFirst();
    }

    @Override
    public void clear() {
        insertionOrder.clear();
    }
}

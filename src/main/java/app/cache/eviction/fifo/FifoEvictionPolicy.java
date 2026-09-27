package app.cache.eviction.fifo;

import app.cache.eviction.EvictionPolicy;
import java.util.ArrayDeque;
import java.util.Queue;

public final class FifoEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Queue<K> insertionOrder = new ArrayDeque<>();

    @Override
    public void onPut(final K key) {
        if (!insertionOrder.contains(key)) {
            insertionOrder.offer(key);
        }
    }

    @Override
    public void onRemove(final K key) {
        insertionOrder.remove(key);
    }

    @Override
    public K evict() {
        return insertionOrder.poll();
    }

    @Override
    public void clear() {
        insertionOrder.clear();
    }
}

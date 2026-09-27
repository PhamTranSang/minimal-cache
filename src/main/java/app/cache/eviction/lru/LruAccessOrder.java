package app.cache.eviction.lru;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LruAccessOrder<K> {

    private final Map<K, LruNode<K>> nodes = new HashMap<>();

    private LruNode<K> head; // Least Recently Used
    private LruNode<K> tail; // Most Recently Used

    public void add(final K key) {
        if (nodes.containsKey(key)) {
            moveToTail(nodes.get(key));
            return;
        }

        final LruNode<K> node = new LruNode<>(key);
        nodes.put(key, node);
        appendToTail(node);
    }

    public void access(final K key) {
        final LruNode<K> node = nodes.get(key);

        if (Objects.nonNull(node)) {
            moveToTail(node);
        }
    }

    public void remove(final K key) {
        final LruNode<K> node = nodes.remove(key);

        if (Objects.nonNull(node)) {
            detach(node);
        }
    }

    public K removeLeastRecentlyUsed() {
        if (Objects.isNull(head)) {
            return null;
        }

        final LruNode<K> lru = head;

        detach(lru);
        nodes.remove(lru.getKey());

        return lru.getKey();
    }

    public void clear() {
        nodes.clear();
        head = null;
        tail = null;
    }

    private void moveToTail(final LruNode<K> node) {
        if (Objects.isNull(node)) {
            return;
        }

        if (node == tail) {
            return;
        }

        detach(node);
        appendToTail(node);
    }

    private void detach(final LruNode<K> node) {
        final LruNode<K> prev = node.getPrev();
        final LruNode<K> next = node.getNext();

        if (Objects.nonNull(prev)) {
            prev.setNext(next);
        } else {
            head = next;
        }

        if (Objects.nonNull(next)) {
            next.setPrev(prev);
        } else {
            tail = prev;
        }

        node.setPrev(null);
        node.setNext(null);
    }

    private void appendToTail(final LruNode<K> node) {
        if (Objects.isNull(tail)) {
            head = node;
            tail = node;
            return;
        }

        node.setPrev(tail);
        node.setNext(null);

        tail.setNext(node);
        tail = node;
    }
}

package app.cache.eviction.lru;

public class LruNode<K> {

    private final K key;

    private LruNode<K> prev;
    private LruNode<K> next;

    public LruNode(final K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public LruNode<K> getPrev() {
        return prev;
    }

    public void setPrev(LruNode<K> prev) {
        this.prev = prev;
    }

    public LruNode<K> getNext() {
        return next;
    }

    public void setNext(LruNode<K> next) {
        this.next = next;
    }
}

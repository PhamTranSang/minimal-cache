package app.cache.eviction.lfu;

final class LfuNode<K> {

    private final K key;

    private int freq = 1;
    private LfuNode<K> prev;
    private LfuNode<K> next;

    public LfuNode(final K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public int getFreq() {
        return freq;
    }

    public void incrementFreq() {
        freq++;
    }

    public LfuNode<K> getPrev() {
        return prev;
    }

    public void setPrev(final LfuNode<K> prev) {
        this.prev = prev;
    }

    public LfuNode<K> getNext() {
        return next;
    }

    public void setNext(final LfuNode<K> next) {
        this.next = next;
    }
}

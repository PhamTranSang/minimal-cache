package app.cache.entry;

public final class CacheEntry<K, V> {

    private final K key;
    private V value;

    private long writeTime;
    private int frequency = 1;

    private CacheEntry<K, V> prev;
    private CacheEntry<K, V> next;

    public CacheEntry(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    public void setValue(final V value) {
        this.value = value;
    }

    public long writeTime() {
        return writeTime;
    }

    public void setWriteTime(final long writeTime) {
        this.writeTime = writeTime;
    }

    public int frequency() {
        return frequency;
    }

    public void incrementFrequency() {
        frequency++;
    }

    CacheEntry<K, V> prev() {
        return prev;
    }

    void setPrev(final CacheEntry<K, V> prev) {
        this.prev = prev;
    }

    CacheEntry<K, V> next() {
        return next;
    }

    void setNext(final CacheEntry<K, V> next) {
        this.next = next;
    }
}

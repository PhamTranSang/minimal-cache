package app.cache.entry;

public final class EntryList<K, V> {

    private CacheEntry<K, V> head;
    private CacheEntry<K, V> tail;

    public void addLast(final CacheEntry<K, V> entry) {
        entry.setPrev(tail);
        entry.setNext(null);

        if (tail == null) {
            head = entry;
        } else {
            tail.setNext(entry);
        }

        tail = entry;
    }

    public void remove(final CacheEntry<K, V> entry) {
        final CacheEntry<K, V> prev = entry.getPrev();
        final CacheEntry<K, V> next = entry.getNext();

        if (prev != null) {
            prev.setNext(next);
        } else {
            head = next;
        }

        if (next != null) {
            next.setPrev(prev);
        } else {
            tail = prev;
        }

        entry.setPrev(null);
        entry.setNext(null);
    }

    public CacheEntry<K, V> removeFirst() {
        if (head == null) {
            return null;
        }

        final CacheEntry<K, V> first = head;
        remove(first);

        return first;
    }

    public void moveToLast(final CacheEntry<K, V> entry) {
        if (entry == tail) {
            return;
        }

        remove(entry);
        addLast(entry);
    }

    public boolean isEmpty() {
        return head == null;
    }

    public void clear() {
        head = null;
        tail = null;
    }
}

package app.cache.eviction.lfu;

final class FrequencyBucket<K> {

    private LfuNode<K> head;
    private LfuNode<K> tail;

    public void addToTail(final LfuNode<K> node) {
        if (tail == null) {
            head = node;
            tail = node;
            return;
        }

        node.setPrev(tail);
        node.setNext(null);

        tail.setNext(node);
        tail = node;
    }

    public void remove(final LfuNode<K> node) {
        final LfuNode<K> prev = node.getPrev();
        final LfuNode<K> next = node.getNext();

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

        node.setPrev(null);
        node.setNext(null);
    }

    public LfuNode<K> removeHead() {
        if (head == null) {
            return null;
        }

        final LfuNode<K> node = head;
        remove(node);

        return node;
    }

    public boolean isEmpty() {
        return head == null;
    }
}

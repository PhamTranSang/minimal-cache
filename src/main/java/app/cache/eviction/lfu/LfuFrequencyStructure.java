package app.cache.eviction.lfu;

import app.cache.exception.EmptyFrequencyBucketException;
import app.cache.exception.MissingFrequencyBucketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class LfuFrequencyStructure<K> {

    private final Map<K, LfuNode<K>> nodes = new HashMap<>();
    private final Map<Integer, FrequencyBucket<K>> buckets = new HashMap<>();

    private int minFreq;

    public void add(final K key) {
        final LfuNode<K> existing = nodes.get(key);

        if (Objects.nonNull(existing)) {
            access(key);
            return;
        }

        final LfuNode<K> node = new LfuNode<>(key);

        nodes.put(key, node);

        buckets
            .computeIfAbsent(1, ignored -> new FrequencyBucket<>())
            .addToTail(node);

        minFreq = 1;
    }

    public void access(final K key) {
        final LfuNode<K> node = nodes.get(key);

        if (Objects.isNull(node)) {
            return;
        }

        final int oldFreq = node.getFreq();
        final FrequencyBucket<K> oldBucket = buckets.get(oldFreq);

        if (oldBucket == null) {
            throw new MissingFrequencyBucketException(oldFreq);
        }
        if (oldBucket.isEmpty()) {
            throw new EmptyFrequencyBucketException(oldFreq);
        }

        oldBucket.remove(node);

        if (oldBucket.isEmpty()) {
            buckets.remove(oldFreq);

            if (minFreq == oldFreq) {
                minFreq = oldFreq + 1;
            }
        }

        node.incrementFreq();

        buckets
            .computeIfAbsent(node.getFreq(), ignored -> new FrequencyBucket<>())
            .addToTail(node);
    }

    public void remove(final K key) {
        final LfuNode<K> node = nodes.get(key);

        if (Objects.isNull(node)) {
            return;
        }

        final int freq = node.getFreq();
        final FrequencyBucket<K> bucket = buckets.get(freq);

        if (bucket == null) {
            throw new MissingFrequencyBucketException(freq);
        }
        if (bucket.isEmpty()) {
            throw new EmptyFrequencyBucketException(freq);
        }

        bucket.remove(node);
        nodes.remove(key);

        if (bucket.isEmpty()) {
            buckets.remove(freq);

            if (freq == minFreq) {
                minFreq = findMinFrequency();
            }
        }
    }

    public K removeLeastFrequentlyUsed() {
        if (nodes.isEmpty()) {
            return null;
        }

        final FrequencyBucket<K> bucket = buckets.get(minFreq);
        if (Objects.isNull(bucket)) {
            throw new MissingFrequencyBucketException(minFreq);
        }

        final LfuNode<K> victim = bucket.removeHead();
        if (Objects.isNull(victim)) {
            throw new EmptyFrequencyBucketException(minFreq);
        }

        nodes.remove(victim.getKey());

        if (bucket.isEmpty()) {
            buckets.remove(minFreq);

            if (nodes.isEmpty()) {
                minFreq = 0;
            } else {
                minFreq = findMinFrequency();
            }
        }

        return victim.getKey();
    }

    public void clear() {
        nodes.clear();
        buckets.clear();
        minFreq = 0;
    }

    private int findMinFrequency() {
        return buckets.keySet()
            .stream()
            .min(Integer::compareTo)
            .orElse(0);
    }
}

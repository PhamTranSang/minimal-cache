package app.cache.eviction.lfu;

import app.cache.entry.CacheEntry;
import app.cache.entry.EntryList;
import java.util.HashMap;
import java.util.Map;

final class LfuFrequencyStructure<K, V> {

    // Each bucket keeps entries of one frequency, oldest arrival at the head.
    private final Map<Integer, EntryList<K, V>> buckets = new HashMap<>();

    private int minFrequency;

    void add(final CacheEntry<K, V> entry) {
        buckets
            .computeIfAbsent(entry.frequency(), ignored -> new EntryList<>())
            .addLast(entry);

        minFrequency = entry.frequency();
    }

    void access(final CacheEntry<K, V> entry) {
        final int oldFrequency = entry.frequency();
        final EntryList<K, V> oldBucket = bucketOf(oldFrequency);

        oldBucket.remove(entry);

        if (oldBucket.isEmpty()) {
            buckets.remove(oldFrequency);

            if (minFrequency == oldFrequency) {
                minFrequency = oldFrequency + 1;
            }
        }

        entry.incrementFrequency();

        buckets
            .computeIfAbsent(entry.frequency(), ignored -> new EntryList<>())
            .addLast(entry);
    }

    void remove(final CacheEntry<K, V> entry) {
        final int frequency = entry.frequency();
        final EntryList<K, V> bucket = bucketOf(frequency);

        bucket.remove(entry);

        if (bucket.isEmpty()) {
            buckets.remove(frequency);

            if (frequency == minFrequency) {
                minFrequency = findMinFrequency();
            }
        }
    }

    CacheEntry<K, V> removeLeastFrequentlyUsed() {
        if (buckets.isEmpty()) {
            return null;
        }

        final EntryList<K, V> bucket = bucketOf(minFrequency);
        final CacheEntry<K, V> victim = bucket.removeFirst();

        if (victim == null) {
            throw new IllegalStateException("frequency bucket is empty: " + minFrequency);
        }

        if (bucket.isEmpty()) {
            buckets.remove(minFrequency);
            minFrequency = findMinFrequency();
        }

        return victim;
    }

    void clear() {
        buckets.clear();
        minFrequency = 0;
    }

    private EntryList<K, V> bucketOf(final int frequency) {
        final EntryList<K, V> bucket = buckets.get(frequency);

        if (bucket == null) {
            throw new IllegalStateException("no frequency bucket found: " + frequency);
        }
        if (bucket.isEmpty()) {
            throw new IllegalStateException("frequency bucket is empty: " + frequency);
        }

        return bucket;
    }

    private int findMinFrequency() {
        return buckets.keySet()
            .stream()
            .min(Integer::compareTo)
            .orElse(0);
    }
}

package app.cache.eviction.lfu;

import app.cache.entry.CacheEntry;
import app.cache.entry.EntryList;
import app.cache.exception.EmptyFrequencyBucketException;
import app.cache.exception.MissingFrequencyBucketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class LfuFrequencyStructure<K, V> {

    // Each bucket keeps entries of one frequency, oldest arrival at the head.
    private final Map<Integer, EntryList<K, V>> buckets = new HashMap<>();

    private int minFreq;

    public void add(final CacheEntry<K, V> entry) {
        buckets
            .computeIfAbsent(entry.getFrequency(), ignored -> new EntryList<>())
            .addLast(entry);

        minFreq = entry.getFrequency();
    }

    public void access(final CacheEntry<K, V> entry) {
        final int oldFreq = entry.getFrequency();
        final EntryList<K, V> oldBucket = bucketOf(oldFreq);

        oldBucket.remove(entry);

        if (oldBucket.isEmpty()) {
            buckets.remove(oldFreq);

            if (minFreq == oldFreq) {
                minFreq = oldFreq + 1;
            }
        }

        entry.incrementFrequency();

        buckets
            .computeIfAbsent(entry.getFrequency(), ignored -> new EntryList<>())
            .addLast(entry);
    }

    public void remove(final CacheEntry<K, V> entry) {
        final int freq = entry.getFrequency();
        final EntryList<K, V> bucket = bucketOf(freq);

        bucket.remove(entry);

        if (bucket.isEmpty()) {
            buckets.remove(freq);

            if (freq == minFreq) {
                minFreq = findMinFrequency();
            }
        }
    }

    public CacheEntry<K, V> removeLeastFrequentlyUsed() {
        if (buckets.isEmpty()) {
            return null;
        }

        final EntryList<K, V> bucket = bucketOf(minFreq);
        final CacheEntry<K, V> victim = bucket.removeFirst();

        if (Objects.isNull(victim)) {
            throw new EmptyFrequencyBucketException(minFreq);
        }

        if (bucket.isEmpty()) {
            buckets.remove(minFreq);
            minFreq = findMinFrequency();
        }

        return victim;
    }

    public void clear() {
        buckets.clear();
        minFreq = 0;
    }

    private EntryList<K, V> bucketOf(final int freq) {
        final EntryList<K, V> bucket = buckets.get(freq);

        if (Objects.isNull(bucket)) {
            throw new MissingFrequencyBucketException(freq);
        }
        if (bucket.isEmpty()) {
            throw new EmptyFrequencyBucketException(freq);
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

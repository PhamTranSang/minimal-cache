package app.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.cache.builder.CacheBuilder;
import app.cache.eviction.EvictionPolicy;
import app.cache.eviction.fifo.FifoEvictionPolicy;
import app.cache.eviction.lfu.LfuEvictionPolicy;
import app.cache.eviction.lru.LruEvictionPolicy;
import app.cache.exception.IncompleteCacheConfigurationException;
import app.cache.exception.InvalidCacheEntryException;
import app.cache.exception.InvalidCapacityException;
import app.cache.exception.InvalidTtlException;
import app.cache.expiration.ttl.TtlExpirationPolicy;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CacheBuilderTest {

    private static final Duration SHORT_TTL = Duration.ofMillis(50);
    private static final long WAIT_PAST_TTL_MILLIS = 100;

    private static Cache<String, String> cache(final int capacity, final EvictionPolicy<String> policy) {
        return CacheBuilder.<String, String>newBuilder()
            .capacity(capacity)
            .evictionPolicy(policy)
            .build();
    }

    private static Cache<String, String> ttlCache(final int capacity, final Duration ttl) {
        return CacheBuilder.<String, String>newBuilder()
            .capacity(capacity)
            .evictionPolicy(new FifoEvictionPolicy<>())
            .expirationPolicy(new TtlExpirationPolicy<>(ttl))
            .build();
    }

    @Nested
    class Configuration {

        @Test
        void missingCapacityIsRejected() {
            final CacheBuilder<String, String> builder = CacheBuilder.<String, String>newBuilder()
                .evictionPolicy(new FifoEvictionPolicy<>());

            assertThrows(IncompleteCacheConfigurationException.class, builder::build);
        }

        @Test
        void missingEvictionPolicyIsRejected() {
            final CacheBuilder<String, String> builder = CacheBuilder.<String, String>newBuilder()
                .capacity(10);

            assertThrows(IncompleteCacheConfigurationException.class, builder::build);
        }

        @Test
        void nonPositiveCapacityIsRejected() {
            assertThrows(InvalidCapacityException.class, () -> cache(0, new FifoEvictionPolicy<>()));
            assertThrows(InvalidCapacityException.class, () -> cache(-1, new FifoEvictionPolicy<>()));
        }

        @Test
        void nonPositiveTtlIsRejected() {
            assertThrows(InvalidTtlException.class, () -> new TtlExpirationPolicy<String>(Duration.ZERO));
            assertThrows(InvalidTtlException.class, () -> new TtlExpirationPolicy<String>(Duration.ofSeconds(-1)));
            assertThrows(InvalidTtlException.class, () -> new TtlExpirationPolicy<String>(null));
        }
    }

    @Nested
    class BasicOperations {

        private final Cache<String, String> cache = cache(3, new FifoEvictionPolicy<>());

        @Test
        void getReturnsStoredValue() {
            cache.put("A", "alpha");

            assertEquals(Optional.of("alpha"), cache.get("A"));
            assertEquals(1, cache.size());
        }

        @Test
        void getMissingKeyReturnsEmpty() {
            assertEquals(Optional.empty(), cache.get("missing"));
        }

        @Test
        void putExistingKeyReplacesValueWithoutGrowing() {
            cache.put("A", "alpha");
            cache.put("A", "alpha-2");

            assertEquals(Optional.of("alpha-2"), cache.get("A"));
            assertEquals(1, cache.size());
        }

        @Test
        void removeReturnsRemovedValue() {
            cache.put("A", "alpha");

            assertEquals(Optional.of("alpha"), cache.remove("A"));
            assertEquals(Optional.empty(), cache.get("A"));
            assertEquals(Optional.empty(), cache.remove("A"));
            assertEquals(0, cache.size());
        }

        @Test
        void clearRemovesAllEntries() {
            cache.put("A", "alpha");
            cache.put("B", "beta");

            cache.clear();

            assertEquals(0, cache.size());
            assertEquals(Optional.empty(), cache.get("A"));
        }

        @Test
        void nullKeyOrValueIsRejected() {
            assertThrows(InvalidCacheEntryException.class, () -> cache.put(null, "value"));
            assertThrows(InvalidCacheEntryException.class, () -> cache.put("A", null));
            assertThrows(InvalidCacheEntryException.class, () -> cache.get(null));
            assertThrows(InvalidCacheEntryException.class, () -> cache.remove(null));
        }
    }

    @Nested
    class Eviction {

        @Test
        void fifoEvictsOldestInsertedKeyEvenIfRead() {
            final Cache<String, String> cache = cache(3, new FifoEvictionPolicy<>());
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("C", "c");
            cache.get("A");

            cache.put("D", "d");

            assertEquals(Optional.empty(), cache.get("A"));
            assertTrue(cache.get("B").isPresent());
            assertTrue(cache.get("C").isPresent());
            assertTrue(cache.get("D").isPresent());
        }

        @Test
        void fifoUpdateDoesNotChangeInsertionOrder() {
            final Cache<String, String> cache = cache(2, new FifoEvictionPolicy<>());
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("A", "a-2");

            cache.put("C", "c");

            assertEquals(Optional.empty(), cache.get("A"));
            assertTrue(cache.get("B").isPresent());
        }

        @Test
        void lruEvictsLeastRecentlyUsedKey() {
            final Cache<String, String> cache = cache(3, new LruEvictionPolicy<>());
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("C", "c");
            cache.get("A");

            cache.put("D", "d");

            assertEquals(Optional.empty(), cache.get("B"));
            assertTrue(cache.get("A").isPresent());
            assertTrue(cache.get("C").isPresent());
            assertTrue(cache.get("D").isPresent());
        }

        @Test
        void lruUpdateMarksKeyAsRecentlyUsed() {
            final Cache<String, String> cache = cache(2, new LruEvictionPolicy<>());
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("A", "a-2");

            cache.put("C", "c");

            assertEquals(Optional.empty(), cache.get("B"));
            assertEquals(Optional.of("a-2"), cache.get("A"));
        }

        @Test
        void lfuEvictsLeastFrequentlyUsedKeyThenOldestInBucket() {
            // Same walkthrough as docs/lfu.md.
            final Cache<String, String> cache = cache(3, new LfuEvictionPolicy<>());
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("C", "c");
            cache.get("A");
            cache.get("B");

            cache.put("D", "d");
            assertEquals(Optional.empty(), cache.get("C"));

            cache.get("D");
            cache.put("E", "e");

            assertEquals(Optional.empty(), cache.get("A"));
            assertTrue(cache.get("B").isPresent());
            assertTrue(cache.get("D").isPresent());
            assertTrue(cache.get("E").isPresent());
        }

        @Test
        void removedKeyIsNoLongerAnEvictionCandidate() {
            final Cache<String, String> cache = cache(2, new LruEvictionPolicy<>());
            cache.put("A", "a");
            cache.put("B", "b");
            cache.remove("A");

            cache.put("C", "c");

            assertEquals(2, cache.size());
            assertTrue(cache.get("B").isPresent());
            assertTrue(cache.get("C").isPresent());
        }

        @Test
        void cacheIsUsableAfterClear() {
            final Cache<String, String> cache = cache(2, new LfuEvictionPolicy<>());
            cache.put("A", "a");
            cache.put("B", "b");
            cache.clear();

            cache.put("C", "c");
            cache.put("D", "d");
            cache.put("E", "e");

            assertEquals(2, cache.size());
            assertEquals(Optional.empty(), cache.get("C"));
        }
    }

    @Nested
    class Ttl {

        @Test
        void entryIsAvailableBeforeTtl() {
            final Cache<String, String> cache = ttlCache(3, Duration.ofMinutes(1));
            cache.put("A", "alpha");

            assertEquals(Optional.of("alpha"), cache.get("A"));
        }

        @Test
        void expiredEntryIsRemovedOnGet() throws InterruptedException {
            final Cache<String, String> cache = ttlCache(3, SHORT_TTL);
            cache.put("A", "alpha");

            Thread.sleep(WAIT_PAST_TTL_MILLIS);

            assertEquals(Optional.empty(), cache.get("A"));
            assertEquals(0, cache.size());
        }

        @Test
        void putAfterExpiryStoresFreshValue() throws InterruptedException {
            final Cache<String, String> cache = ttlCache(3, SHORT_TTL);
            cache.put("A", "alpha");

            Thread.sleep(WAIT_PAST_TTL_MILLIS);
            cache.put("A", "alpha-2");

            assertEquals(Optional.of("alpha-2"), cache.get("A"));
            assertEquals(1, cache.size());
        }

        @Test
        void expiredEntriesAreCleanedBeforeEvictingValidOnes() throws InterruptedException {
            final Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .capacity(2)
                .evictionPolicy(new FifoEvictionPolicy<>())
                .expirationPolicy(new TtlExpirationPolicy<>(Duration.ofMillis(200)))
                .build();
            cache.put("A", "a");
            Thread.sleep(250);
            cache.put("B", "b");

            cache.put("C", "c");

            // A expired, so it is cleaned instead of evicting the still-valid B.
            assertEquals(Optional.empty(), cache.get("A"));
            assertTrue(cache.get("B").isPresent());
            assertTrue(cache.get("C").isPresent());
        }
    }
}

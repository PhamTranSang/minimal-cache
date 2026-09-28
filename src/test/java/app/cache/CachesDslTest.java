package app.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.cache.Caches.CacheConfiguration;
import app.cache.exception.IncompleteCacheConfigurationException;
import app.cache.exception.InvalidCacheConfigurationException;
import app.cache.exception.InvalidCacheEntryException;
import app.cache.exception.InvalidCapacityException;
import app.cache.exception.InvalidTtlException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CachesDslTest {

    private static final Duration SHORT_TTL = Duration.ofMillis(50);
    private static final long WAIT_PAST_TTL_MILLIS = 100;

    @Nested
    class Configuration {

        @Test
        void nullConfigurationIsRejected() {
            assertThrows(InvalidCacheConfigurationException.class, () -> Caches.create(null));
        }

        @Test
        void configurationMustReturnProvidedInstance() {
            assertThrows(InvalidCacheConfigurationException.class, () -> Caches.create(config -> null));
        }

        @Test
        void missingEvictionPolicyIsRejected() {
            assertThrows(IncompleteCacheConfigurationException.class, () -> Caches.create(config -> config.capacity(10)));
        }

        @Test
        void choosingSecondEvictionPolicyIsRejected() {
            assertThrows(InvalidCacheConfigurationException.class, () -> Caches.create(config -> config.lru().lfu()));
        }

        @Test
        void nonPositiveCapacityIsRejectedOnBuild() {
            final CacheConfiguration config = Caches.create(c -> c.capacity(0).fifo());

            assertThrows(InvalidCapacityException.class, config::build);
        }

        @Test
        void invalidTtlIsRejectedOnBuild() {
            assertThrows(InvalidTtlException.class, Caches.create(c -> c.fifo().ttl(Duration.ZERO))::build);
            assertThrows(InvalidTtlException.class, Caches.create(c -> c.fifo().ttl(null))::build);
        }

        @Test
        void defaultCapacityIsOneHundred() {
            final Cache<Integer, Integer> cache = Caches.create(CacheConfiguration::fifo).build();

            for (int i = 0; i <= 100; i++) {
                cache.put(i, i);
            }

            assertEquals(100, cache.size());
            assertEquals(Optional.empty(), cache.get(0));
            assertEquals(Optional.of(100), cache.get(100));
        }

        @Test
        void eachBuildCreatesIndependentCache() {
            final CacheConfiguration config = Caches.create(c -> c.capacity(2).lru());
            final Cache<String, String> first = config.build();
            final Cache<String, String> second = config.build();

            first.put("A", "a");
            first.put("B", "b");
            second.put("C", "c");
            second.put("D", "d");
            second.put("E", "e");

            assertEquals(2, first.size());
            assertTrue(first.get("A").isPresent());
            assertTrue(first.get("B").isPresent());
            assertEquals(Optional.empty(), second.get("C"));
        }
    }

    @Nested
    class BasicOperations {

        private final Cache<String, String> cache = Caches.create(c -> c.capacity(3).fifo()).build();

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
            final Cache<String, String> cache = Caches.create(c -> c.capacity(3).fifo()).build();
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
        void lruEvictsLeastRecentlyUsedKey() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(3).lru()).build();
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
        void lfuEvictsLeastFrequentlyUsedKeyThenOldestInBucket() {
            // Same walkthrough as docs/lfu.md.
            final Cache<String, String> cache = Caches.create(c -> c.capacity(3).lfu()).build();
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
    }

    @Nested
    class Ttl {

        @Test
        void entryIsAvailableBeforeTtl() {
            final Cache<String, String> cache = Caches.create(c -> c.lru().ttl(Duration.ofMinutes(1))).build();
            cache.put("A", "alpha");

            assertEquals(Optional.of("alpha"), cache.get("A"));
        }

        @Test
        void expiredEntryIsRemovedOnGet() throws InterruptedException {
            final Cache<String, String> cache = Caches.create(c -> c.lru().ttl(SHORT_TTL)).build();
            cache.put("A", "alpha");

            Thread.sleep(WAIT_PAST_TTL_MILLIS);

            assertEquals(Optional.empty(), cache.get("A"));
            assertEquals(0, cache.size());
        }

        @Test
        void withoutTtlEntriesDoNotExpire() throws InterruptedException {
            final Cache<String, String> cache = Caches.create(CacheConfiguration::lru).build();
            cache.put("A", "alpha");

            Thread.sleep(WAIT_PAST_TTL_MILLIS);

            assertEquals(Optional.of("alpha"), cache.get("A"));
        }

        @Test
        void expiredEntriesAreCleanedBeforeEvictingValidOnes() throws InterruptedException {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).lru().ttl(Duration.ofMillis(200))).build();
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

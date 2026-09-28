package app.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.cache.Caches.CacheConfiguration;
import app.cache.exception.CacheException;
import app.cache.exception.IncompleteCacheConfigurationException;
import app.cache.exception.InvalidCacheConfigurationException;
import app.cache.exception.InvalidCacheEntryException;
import app.cache.exception.InvalidCapacityException;
import app.cache.exception.InvalidTtlException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CachesDslTest {

    private static final Duration TTL = Duration.ofMinutes(5);

    @Nested
    class PublicContract {

        @Test
        void callerErrorsShareCacheExceptionBase() {
            final Cache<String, String> cache = Caches.create(CacheConfiguration::lru).build();

            assertThrows(CacheException.class, () -> Caches.create(null));
            assertThrows(CacheException.class, () -> Caches.create(c -> c.capacity(10)));
            assertThrows(CacheException.class, Caches.create(c -> c.capacity(0).lru())::build);
            assertThrows(CacheException.class, Caches.create(c -> c.lru().ttl(Duration.ZERO))::build);
            assertThrows(CacheException.class, () -> cache.put(null, "value"));
        }

        @Test
        void callersCanImplementCache() {
            // Cache is not sealed, so callers can write their own fakes or decorators.
            final Cache<String, String> fake = new Cache<>() {
                private final Map<String, String> map = new HashMap<>();

                @Override
                public Optional<String> get(final String key) {
                    return Optional.ofNullable(map.get(key));
                }

                @Override
                public void put(final String key, final String value) {
                    map.put(key, value);
                }

                @Override
                public Optional<String> remove(final String key) {
                    return Optional.ofNullable(map.remove(key));
                }

                @Override
                public int size() {
                    return map.size();
                }

                @Override
                public void clear() {
                    map.clear();
                }
            };

            fake.put("A", "alpha");

            assertEquals(Optional.of("alpha"), fake.get("A"));
        }
    }

    @Nested
    class Configuration {

        @Test
        void nullConfigurationIsRejected() {
            assertThrows(InvalidCacheConfigurationException.class, () -> Caches.create(null));
        }

        @Test
        void configurationMustReturnProvidedInstance() {
            assertThrows(InvalidCacheConfigurationException.class, () -> Caches.create(c -> null));
        }

        @Test
        void missingEvictionPolicyIsRejected() {
            assertThrows(IncompleteCacheConfigurationException.class, () -> Caches.create(c -> c.capacity(10)));
        }

        @Test
        void choosingSecondEvictionPolicyIsRejected() {
            assertThrows(InvalidCacheConfigurationException.class, () -> Caches.create(c -> c.lru().lfu()));
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
        void nullTickerIsRejectedOnBuildWhenTtlIsConfigured() {
            final CacheConfiguration config = Caches.create(c -> c.fifo().ttl(TTL).ticker(null));

            assertThrows(InvalidCacheConfigurationException.class, config::build);
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
            assertEquals(Optional.of("a"), first.get("A"));
            assertEquals(Optional.of("b"), first.get("B"));
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
            assertEquals(Optional.of("b"), cache.get("B"));
            assertEquals(Optional.of("c"), cache.get("C"));
            assertEquals(Optional.of("d"), cache.get("D"));
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
            assertEquals(Optional.of("a"), cache.get("A"));
            assertEquals(Optional.of("c"), cache.get("C"));
            assertEquals(Optional.of("d"), cache.get("D"));
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
            assertEquals(Optional.of("b"), cache.get("B"));
            assertEquals(Optional.of("d"), cache.get("D"));
            assertEquals(Optional.of("e"), cache.get("E"));
        }

        @Test
        void fifoUpdateDoesNotChangeInsertionOrder() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).fifo()).build();
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("A", "a-2");

            cache.put("C", "c");

            assertEquals(Optional.empty(), cache.get("A"));
            assertEquals(Optional.of("b"), cache.get("B"));
        }

        @Test
        void fifoReAddedKeyMovesToBackOfQueue() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).fifo()).build();
            cache.put("A", "a");
            cache.put("B", "b");
            cache.remove("A");
            cache.put("A", "a-2");

            cache.put("C", "c");

            assertEquals(Optional.empty(), cache.get("B"));
            assertEquals(Optional.of("a-2"), cache.get("A"));
        }

        @Test
        void lfuUpdateCountsAsUse() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).lfu()).build();
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("A", "a-2");

            cache.put("C", "c");

            assertEquals(Optional.empty(), cache.get("B"));
            assertEquals(Optional.of("a-2"), cache.get("A"));
        }

        @Test
        void lruUpdateMarksKeyAsRecentlyUsed() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).lru()).build();
            cache.put("A", "a");
            cache.put("B", "b");
            cache.put("A", "a-2");

            cache.put("C", "c");

            assertEquals(Optional.empty(), cache.get("B"));
            assertEquals(Optional.of("a-2"), cache.get("A"));
        }

        @Test
        void removedKeyIsNoLongerAnEvictionCandidate() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).lru()).build();
            cache.put("A", "a");
            cache.put("B", "b");
            cache.remove("A");

            cache.put("C", "c");

            assertEquals(2, cache.size());
            assertEquals(Optional.of("b"), cache.get("B"));
            assertEquals(Optional.of("c"), cache.get("C"));
        }

        @Test
        void cacheIsUsableAfterClear() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).lfu()).build();
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

        private final FakeTicker ticker = new FakeTicker();

        @Test
        void entryIsAvailableBeforeTtl() {
            final Cache<String, String> cache = Caches.create(c -> c.lru().ttl(TTL).ticker(ticker)).build();
            cache.put("A", "alpha");

            ticker.advance(TTL.minusNanos(1));

            assertEquals(Optional.of("alpha"), cache.get("A"));
        }

        @Test
        void expiredEntryIsRemovedOnGet() {
            final Cache<String, String> cache = Caches.create(c -> c.lru().ttl(TTL).ticker(ticker)).build();
            cache.put("A", "alpha");

            ticker.advance(TTL);

            assertEquals(Optional.empty(), cache.get("A"));
            assertEquals(0, cache.size());
        }

        @Test
        void getDoesNotExtendTtl() {
            final Cache<String, String> cache = Caches.create(c -> c.lru().ttl(TTL).ticker(ticker)).build();
            cache.put("A", "alpha");

            ticker.advance(Duration.ofMinutes(4));
            cache.get("A");
            ticker.advance(Duration.ofMinutes(1));

            assertEquals(Optional.empty(), cache.get("A"));
        }

        @Test
        void putAfterExpiryStoresFreshValue() {
            final Cache<String, String> cache = Caches.create(c -> c.lru().ttl(TTL).ticker(ticker)).build();
            cache.put("A", "alpha");

            ticker.advance(TTL);
            cache.put("A", "alpha-2");

            assertEquals(Optional.of("alpha-2"), cache.get("A"));
            assertEquals(1, cache.size());
        }

        @Test
        void withoutTtlEntriesDoNotExpire() {
            final Cache<String, String> cache = Caches.create(c -> c.lru().ticker(ticker)).build();
            cache.put("A", "alpha");

            ticker.advance(Duration.ofDays(365));

            assertEquals(Optional.of("alpha"), cache.get("A"));
        }

        @Test
        void expiredEntriesAreCleanedBeforeEvictingValidOnes() {
            final Cache<String, String> cache = Caches.create(c -> c.capacity(2).lru().ttl(TTL).ticker(ticker)).build();
            cache.put("A", "a");
            ticker.advance(TTL);
            cache.put("B", "b");

            cache.put("C", "c");

            // A expired, so it is cleaned instead of evicting the still-valid B.
            assertEquals(Optional.empty(), cache.get("A"));
            assertEquals(Optional.of("b"), cache.get("B"));
            assertEquals(Optional.of("c"), cache.get("C"));
        }
    }
}

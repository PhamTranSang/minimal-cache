package app.cache;

import app.cache.config.CacheConfig;
import app.cache.eviction.EvictionPolicy;
import app.cache.eviction.fifo.FifoEvictionPolicy;
import app.cache.eviction.lfu.LfuEvictionPolicy;
import app.cache.eviction.lru.LruEvictionPolicy;
import app.cache.exception.IncompleteCacheConfigurationException;
import app.cache.exception.InvalidCacheConfigurationException;
import app.cache.expiration.ExpirationPolicy;
import app.cache.expiration.ttl.NoExpirationPolicy;
import app.cache.expiration.ttl.TtlExpirationPolicy;
import java.time.Duration;
import java.util.function.UnaryOperator;

public final class Caches {

    private Caches() {
    }

    public static CacheConfiguration create(final UnaryOperator<CacheConfiguration> configuration) {
        if (configuration == null) {
            throw new InvalidCacheConfigurationException("configuration must not be null");
        }

        final CacheConfiguration config = new CacheConfiguration();
        final CacheConfiguration result = configuration.apply(config);

        if (result != config) {
            throw new InvalidCacheConfigurationException("configuration must return the provided configuration");
        }

        config.validate();
        return config;
    }

    public static final class CacheConfiguration {

        private int capacity = 100;
        private EvictionType evictionType;
        private Duration ttl;
        private boolean ttlConfigured;
        private Ticker ticker = Ticker.systemTicker();

        private CacheConfiguration() {
        }

        public CacheConfiguration capacity(final int capacity) {
            this.capacity = capacity;
            return this;
        }

        public CacheConfiguration fifo() {
            return eviction(EvictionType.FIFO);
        }

        public CacheConfiguration lru() {
            return eviction(EvictionType.LRU);
        }

        public CacheConfiguration lfu() {
            return eviction(EvictionType.LFU);
        }

        public CacheConfiguration ttl(final Duration ttl) {
            this.ttl = ttl;
            this.ttlConfigured = true;
            return this;
        }

        public CacheConfiguration ticker(final Ticker ticker) {
            this.ticker = ticker;
            return this;
        }

        private CacheConfiguration eviction(final EvictionType kind) {
            if (evictionType != null) {
                throw new InvalidCacheConfigurationException("eviction policy already configured");
            }

            evictionType = kind;
            return this;
        }

        private void validate() {
            if (evictionType == null) {
                throw new IncompleteCacheConfigurationException("eviction policy must be configured");
            }
        }

        public <K, V> Cache<K, V> build() {
            validate();

            final EvictionPolicy<K, V> evictionPolicy = switch (evictionType) {
                case FIFO -> new FifoEvictionPolicy<>();
                case LRU -> new LruEvictionPolicy<>();
                case LFU -> new LfuEvictionPolicy<>();
            };

            final ExpirationPolicy<K, V> expirationPolicy = ttlConfigured
                ? new TtlExpirationPolicy<>(ttl, ticker)
                : new NoExpirationPolicy<>();

            return new InMemoryCache<>(new CacheConfig<>(capacity, evictionPolicy, expirationPolicy));
        }
    }

    private enum EvictionType {
        FIFO,
        LRU,
        LFU
    }
}

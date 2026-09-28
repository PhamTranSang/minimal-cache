package app.cache;

import app.cache.config.CacheConfig;
import app.cache.entry.CacheEntry;
import app.cache.exception.InvalidCacheConfigurationException;
import app.cache.exception.InvalidCacheEntryException;
import app.cache.exception.MissingEvictionVictimException;
import app.cache.exception.UnknownEvictionVictimException;
import app.cache.expiration.ttl.NoExpirationPolicy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InMemoryCache<K, V> implements Cache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCache.class);

    // Single source of truth: policies keep their metadata on the entries, not in maps of their own.
    private final Map<K, CacheEntry<K, V>> entries = new HashMap<>();
    private final CacheConfig<K, V> config;

    public InMemoryCache(final CacheConfig<K, V> config) {
        if (config == null) {
            throw new InvalidCacheConfigurationException("config must not be null");
        }

        this.config = config;
    }

    @Override
    public Optional<V> get(final K key) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }

        final CacheEntry<K, V> entry = entries.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        if (config.expirationPolicy().isExpired(entry)) {
            removeEntry(entry);
            log.debug("Expired cache entry removed, key={}", key);
            return Optional.empty();
        }

        config.evictionPolicy().onAccess(entry);

        return Optional.of(entry.getValue());
    }

    @Override
    public void put(final K key, final V value) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }
        if (value == null) {
            throw new InvalidCacheEntryException("value must not be null");
        }

        final CacheEntry<K, V> existing = entries.get(key);

        if (existing != null) {

            if (config.expirationPolicy().isExpired(existing)) {
                removeEntry(existing);
                log.debug("Expired cache entry removed, key={}", key);
            } else {
                existing.setValue(value);

                config.expirationPolicy().onWrite(existing);
                config.evictionPolicy().onAccess(existing);

                return;
            }
        }

        if (entries.size() >= config.capacity() && !(config.expirationPolicy() instanceof NoExpirationPolicy)) {
            removeExpiredEntries();
        }

        if (entries.size() >= config.capacity()) {
            evict();
        }

        final CacheEntry<K, V> entry = new CacheEntry<>(key, value);
        entries.put(key, entry);

        config.expirationPolicy().onWrite(entry);
        config.evictionPolicy().onAdd(entry);
    }

    @Override
    public Optional<V> remove(final K key) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }

        final CacheEntry<K, V> entry = entries.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        removeEntry(entry);

        return Optional.of(entry.getValue());
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public void clear() {
        entries.clear();

        config.evictionPolicy().clear();
    }

    private void evict() {
        final CacheEntry<K, V> victim = config.evictionPolicy().evict();

        if (victim == null) {
            throw new MissingEvictionVictimException();
        }

        if (!entries.remove(victim.getKey(), victim)) {
            throw new UnknownEvictionVictimException(victim.getKey());
        }

        log.debug("Evicted cache entry, key={}", victim.getKey());
    }

    private void removeEntry(final CacheEntry<K, V> entry) {
        entries.remove(entry.getKey());

        config.evictionPolicy().onRemove(entry);
    }

    private void removeExpiredEntries() {
        final Iterator<CacheEntry<K, V>> iterator = entries.values().iterator();

        while (iterator.hasNext()) {
            final CacheEntry<K, V> entry = iterator.next();

            if (!config.expirationPolicy().isExpired(entry)) {
                continue;
            }

            iterator.remove();

            config.evictionPolicy().onRemove(entry);
            log.debug("Expired cache entry removed, key={}", entry.getKey());
        }
    }
}

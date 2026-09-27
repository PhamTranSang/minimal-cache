package app.cache;

import app.cache.config.CacheConfig;
import app.cache.exception.InvalidCacheConfigurationException;
import app.cache.exception.InvalidCacheEntryException;
import app.cache.exception.MissingEvictionVictimException;
import app.cache.exception.UnknownEvictionVictimException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InMemoryCache<K, V> implements Cache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCache.class);

    private final Map<K, V> entries = new HashMap<>();
    private final CacheConfig<K> config;

    public InMemoryCache(final CacheConfig<K> config) {
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

        final V value = entries.get(key);

        if (value == null) {
            return Optional.empty();
        }

        if (config.expirationPolicy().isExpired(key)) {
            removeEntry(key);
            log.debug("Expired cache entry removed, key={}", key);
            return Optional.empty();
        }

        config.evictionPolicy().onGet(key);

        return Optional.of(value);
    }

    @Override
    public void put(final K key, final V value) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }
        if (value == null) {
            throw new InvalidCacheEntryException("value must not be null");
        }

        if (entries.containsKey(key)) {

            if (config.expirationPolicy().isExpired(key)) {
                removeEntry(key);
                log.debug("Expired cache entry removed, key={}", key);
            } else {
                entries.put(key, value);

                config.evictionPolicy().onPut(key);
                config.expirationPolicy().onPut(key);

                return;
            }
        }

        if (entries.size() >= config.capacity()) {
            removeExpiredEntries();
        }

        if (entries.size() >= config.capacity()) {
            evict();
        }

        entries.put(key, value);

        config.evictionPolicy().onPut(key);
        config.expirationPolicy().onPut(key);
    }

    @Override
    public Optional<V> remove(final K key) {
        if (key == null) {
            throw new InvalidCacheEntryException("key must not be null");
        }

        return Optional.ofNullable(removeEntry(key));
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public void clear() {
        entries.clear();

        config.evictionPolicy().clear();
        config.expirationPolicy().clear();
    }

    private void evict() {
        final K victim = config.evictionPolicy().evict();

        if (victim == null) {
            throw new MissingEvictionVictimException();
        }

        final V removed = entries.remove(victim);

        if (removed == null) {
            throw new UnknownEvictionVictimException(victim);
        }

        config.expirationPolicy().onRemove(victim);
        log.debug("Evicted cache entry, key={}", victim);
    }

    private V removeEntry(final K key) {
        final V removed = entries.remove(key);

        if (removed == null) {
            return null;
        }

        config.evictionPolicy().onRemove(key);
        config.expirationPolicy().onRemove(key);

        return removed;
    }

    private void removeExpiredEntries() {
        final Iterator<K> iterator =
                entries.keySet().iterator();

        while (iterator.hasNext()) {
            final K key = iterator.next();

            if (!config.expirationPolicy().isExpired(key)) {
                continue;
            }

            iterator.remove();

            config.evictionPolicy().onRemove(key);
            config.expirationPolicy().onRemove(key);
            log.debug("Expired cache entry removed, key={}", key);
        }
    }
}

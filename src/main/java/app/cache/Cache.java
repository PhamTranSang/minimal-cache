package app.cache;

import java.util.Optional;

public sealed interface Cache<K, V> permits InMemoryCache {

    Optional<V> get(K key);

    void put(K key, V value);

    Optional<V> remove(K key);

    int size();

    void clear();
}

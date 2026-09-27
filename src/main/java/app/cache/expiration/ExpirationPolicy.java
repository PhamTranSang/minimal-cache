package app.cache.expiration;

import app.cache.expiration.ttl.NoExpirationPolicy;
import app.cache.expiration.ttl.TtlExpirationPolicy;

public sealed interface ExpirationPolicy<K> permits NoExpirationPolicy, TtlExpirationPolicy {

    void onPut(K key);

    boolean isExpired(K key);

    void onRemove(K key);

    void clear();
}

package app.cache.expiration;

import app.cache.entry.CacheEntry;
import app.cache.expiration.ttl.NoExpirationPolicy;
import app.cache.expiration.ttl.TtlExpirationPolicy;

public sealed interface ExpirationPolicy<K, V> permits NoExpirationPolicy, TtlExpirationPolicy {

    void onWrite(CacheEntry<K, V> entry);

    boolean isExpired(CacheEntry<K, V> entry);
}
